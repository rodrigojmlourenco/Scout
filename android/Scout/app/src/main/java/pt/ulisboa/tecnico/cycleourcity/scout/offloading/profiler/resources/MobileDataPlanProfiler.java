package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.resources;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.cycleourcity.scout.ScoutApplication;

/**
 * Created by rodrigo.jm.lourenco on 28/07/2015.
 */
public class MobileDataPlanProfiler extends BroadcastReceiver{

    protected boolean VERBOSE = true;
    private final String LOG_TAG = this.getClass().getSimpleName();

    public interface DataPlanStateFields {
        public final static String
                NAME = "name",
                PREFS_NAME = "mobile_data",

        //Immutable Fields
        DATA_PLAN = "dataPlan",
                DATA_LIMIT = "dataLimit",

        //Marks the last timestamp
        TIME = "time_since_shutdown",

        //Before Last Shutdown
        PRE_USAGE       = "pre_data_usage",
                PRE_DOWNLOAD    = "pre_downloaded", //useless
                PRE_UPLOAD      = "pre_uploaded",//useless

        //After Last Shutdown
        POST_USAGE = "post_data_usage",
                POST_DOWNLOAD   = "post_downloaded",//useless
                POST_UPLOAD     = "post_uploaded",//useless

        //Real data
        CONSUMED_DATA   = "dataUsage";
    }


    private long
            maxDataUsage,           //IMMUTABLE
            limitDataUsage,         //IMMUTABLE
            preShutdownDownloaded,
            preShutdownUploaded,
            preShutdownUsage,
            postShutdownDownloaded,
            postShutdownUploaded,
            postShutdownUsage,
            pseudoRealDataUsage;

    private long lastUpdate;

    private boolean hasState = false;

    private Gson gson = new Gson();
    private JsonParser parser = new JsonParser();

    //Persistent Storage
    private final Context appContext;
    private SharedPreferences profilingPrefs;

    //Async Monitoring
    private ScheduledFuture monitorHandler = null;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Runnable monitor = new Runnable() {
        @Override
        public void run() {
            updateState();
            storeState();
        }
    };

    public MobileDataPlanProfiler(){
        appContext = ScoutApplication.getContext();

        init();

    }

    public MobileDataPlanProfiler(Context context) {

        appContext = context;

        init();
    }

    private void init(){
        profilingPrefs =
                appContext.getSharedPreferences(ScoutProfiling.PREFERENCES, Context.MODE_PRIVATE);

        fetchState();
        updateState();
        storeState();

    }


    public void updateState(){

        postShutdownDownloaded = TrafficStats.getMobileRxBytes();
        postShutdownUploaded = TrafficStats.getMobileTxBytes();

        postShutdownUsage = postShutdownDownloaded + postShutdownUploaded;

        pseudoRealDataUsage = preShutdownUsage + postShutdownUsage;
    }


    /*
     ****************************************************************
     * Internal Stage - Support Persistence                         *
     ****************************************************************
     */

    private JsonObject parseState(){

        if(!hasState) return null;

        JsonObject state = new JsonObject();

        state.addProperty(DataPlanStateFields.NAME, DataPlanStateFields.PREFS_NAME);
        state.addProperty(DataPlanStateFields.TIME, System.nanoTime());

        //Immutable
        state.addProperty(DataPlanStateFields.DATA_PLAN, maxDataUsage);
        state.addProperty(DataPlanStateFields.DATA_LIMIT, limitDataUsage);

        //Pseudo-Real Total data consumed
        state.addProperty(DataPlanStateFields.CONSUMED_DATA, pseudoRealDataUsage);

        //Pre-Shutdown
        state.addProperty(DataPlanStateFields.PRE_USAGE, preShutdownUsage);
        state.addProperty(DataPlanStateFields.PRE_UPLOAD, preShutdownUploaded);
        state.addProperty(DataPlanStateFields.PRE_DOWNLOAD, preShutdownDownloaded);

        //Post-Shutdown
        state.addProperty(DataPlanStateFields.POST_USAGE, preShutdownUsage);
        state.addProperty(DataPlanStateFields.POST_UPLOAD, postShutdownUploaded);
        state.addProperty(DataPlanStateFields.POST_DOWNLOAD, postShutdownDownloaded);

        return state;
    }

    private void logState(){
        if(hasState)
            Log.d(this.getClass().getSimpleName(), gson.toJson(parseState()));
        else
            Log.w(this.getClass().getSimpleName(), "Nothing found in shared prefs");
    }

    private void storeState(){

        String state = gson.toJson(parseState());

        SharedPreferences.Editor prefsEditor = profilingPrefs.edit();
        prefsEditor.putString(ScoutProfiling.DATA_PLAN_PREFS, state);
        prefsEditor.commit();

        if(VERBOSE) Log.d(LOG_TAG, state);
    }

    private void fetchState() {

        String stateAsString = profilingPrefs.getString(ScoutProfiling.DATA_PLAN_PREFS, null);

        if(stateAsString  == null) {
            hasState = false;
            return;
        }


        JsonObject state = (JsonObject) parser.parse(stateAsString);

        //Time Since Shutdown
        lastUpdate          = state.get(DataPlanStateFields.TIME).getAsLong();

        //Immutable Values
        maxDataUsage        = state.get(DataPlanStateFields.DATA_PLAN).getAsLong();
        limitDataUsage      = state.get(DataPlanStateFields.DATA_LIMIT).getAsLong();

        //Pre-Shutdown
        preShutdownUploaded     = state.get(DataPlanStateFields.PRE_UPLOAD).getAsLong();
        preShutdownDownloaded   = state.get(DataPlanStateFields.PRE_DOWNLOAD).getAsLong();
        preShutdownUsage        = state.get(DataPlanStateFields.PRE_USAGE).getAsLong();

        //Post-Shutdown
        postShutdownUsage       = state.get(DataPlanStateFields.POST_USAGE).getAsLong();
        postShutdownUploaded    = state.get(DataPlanStateFields.POST_UPLOAD).getAsLong();
        postShutdownDownloaded  = state.get(DataPlanStateFields.POST_DOWNLOAD).getAsLong();

        //Pseudo-Real Total
        pseudoRealDataUsage     = state.get(DataPlanStateFields.CONSUMED_DATA).getAsLong();

        hasState = true;
    }

    public void teardown(){
        updateState();
        storeState();
    }

    /*
     ****************************************************************
     * Shutdown handling                                            *
     ****************************************************************
     * This is necessary as the TrafficStats values are reset after *
     * a device boot.                                               *
     ****************************************************************
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(action.equals(Intent.ACTION_SHUTDOWN)) { //Shutdown caught

            updateState();

            lastUpdate              = 0;
            preShutdownUsage        = pseudoRealDataUsage;
            preShutdownUploaded     = 0;
            preShutdownDownloaded   = 0;

            storeState();

        }
    }

    /*
     ****************************************************************
     * Async Profiling                                              *
     ****************************************************************
     */
    public void startMonitoring(){

        if(!hasState) return; //TODO: throw exception

        fetchState();

        monitorHandler = scheduler.scheduleAtFixedRate(
                monitor,
                10,
                10,
                TimeUnit.SECONDS);
    }

    public void stopMonitoring(){
        if(monitorHandler!=null){

            scheduler.shutdown();
            try{
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!scheduler.awaitTermination(60, TimeUnit.SECONDS))
                        System.err.println("Pool did not terminate");
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                scheduler.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }
}
