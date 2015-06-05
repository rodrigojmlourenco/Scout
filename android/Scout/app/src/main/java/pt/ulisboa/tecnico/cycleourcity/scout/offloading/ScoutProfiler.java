package pt.ulisboa.tecnico.cycleourcity.scout.offloading;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.AdaptiveOffloadingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.ProfilerIsAlreadyRunningException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.exceptions.EnergyPropertyNotSupportedException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.resources.EnergyProfiler;

/**
 * ScoutProfiler is the application profiler. It is responsible for monitoring the application's
 * state, more specifically its energy consumption levels and the current remaining battery.
 * <br>
 * The ScoutProfiler performs coarse-grained resource profiling, at the application level.
 * <br>
 * Ultimately the information generated by the ScoutProfiler is to be used by the OffloadingDecisionEngine,
 * as to determine the when it is relevant to perform computational offloading.
 * <br>
 * This component is coordinated and made available to other components by the AdaptiveOffloadingManager.
 *
 * @see OffloadingDecisionEngine
 *
 */
public class ScoutProfiler {

    //TODO: remover antes da release
    public static boolean VERBOSE = false;
    private final String LOG_TAG = AdaptiveOffloadingManager.LOG_TAG;
    public final String NAME_TAG = this.getClass().getSimpleName();

    //Sync Locks
    private final Object lockState = new Object(), lockValues = new Object();

    //Resource Profiling
    private EnergyProfiler sensingEProf;

    //Profiler State
    private boolean isProfiling = false;

    //Logging
    private boolean activeLogging = false;

    //Execution Rate
    public static final int DEFAULT_PROFILING_RATE = 1*1000;//ms
    private int profilingRate = DEFAULT_PROFILING_RATE;
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private Future executorHandler;

    //Singleton
    private static ScoutProfiler APP_PROFILER = null;

    protected static ScoutProfiler getInstance(Context ctx){
        if(APP_PROFILER==null){
            synchronized (ScoutProfiler.class){
                if(APP_PROFILER==null)
                    APP_PROFILER = new ScoutProfiler(ctx);
            }
        }
        return APP_PROFILER;
    }

    private ScoutProfiler(Context context){

        sensingEProf = new EnergyProfiler.SensingEnergyProfiler(
                context,
                context.getSharedPreferences(EnergyProfiler.PREFS_NAME, Context.MODE_PRIVATE));

    }

    protected boolean isProfiling(){
        synchronized (lockState) {
            return isProfiling;
        }
    }


    protected void setProfilingRate(int rateMillis) throws AdaptiveOffloadingException{

        synchronized (lockState) {
            if(isProfiling())
                throw new ProfilerIsAlreadyRunningException();
        }

        this.profilingRate = rateMillis;
    }

    protected void startProfiling(){

        synchronized (lockState) {
            if(isProfiling()) {
                if(VERBOSE) Log.w(LOG_TAG, "[ScoutProfiler:startProfiling] ScoutProfiler is already running");
                return;
            }else
                isProfiling = true;
        }

        if(VERBOSE) Log.w(LOG_TAG,
                "[ScoutProfiler:startProfiling] Initiated a profiling session, to run every "+profilingRate+"ms...");


        disableActiveLogging();
        executorHandler = executorService.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {

            synchronized (lockValues) {
                sensingEProf.profile();
            }

            if(activeLogging) OffloadingLogger.log(NAME_TAG, dumpInfo());

            }
        }, 0, profilingRate, TimeUnit.MILLISECONDS);
    }

    protected void stopProfiling(){

        synchronized (lockState) {
            if(isProfiling()){
                if(VERBOSE) Log.w(LOG_TAG, "[ScoutProfiler:stopProfiling] Stopping ScoutProfiler");
                executorHandler.cancel(true);
                isProfiling = false;
            }else
            if(VERBOSE) Log.w(LOG_TAG, "[ScoutProfiler:stopProfiling] ScoutProfiler is already stopped");

        }
    }

    protected void destroy(){
        if(VERBOSE) Log.w(LOG_TAG, "[ScoutProfiler:destroy] ScoutProfiler has been destroyed!");
        executorService.shutdownNow();
    }

    public long getSensingAverageCurrent(){

        synchronized (lockValues) {
            try {
                return this.sensingEProf.getCurrentAverage();
            } catch (EnergyPropertyNotSupportedException e) {
                return -1;
            }
        }
    }

    public int getBatteryCapacity(){
        synchronized (lockValues) {
            try {
                return this.sensingEProf.getCapacity();
            } catch (EnergyPropertyNotSupportedException e) {
                return 100;
            }
        }
    }

    public boolean isCharging(){
        synchronized (lockValues) {
            return this.sensingEProf.isCharging();
        }
    }

    public boolean isFull(){
        synchronized (lockValues) {
            return this.sensingEProf.isFull();
        }
    }

    public String dumpInfo(){
        return "{ name: \""+getClass().getSimpleName()+"\", "+
                "battery:"+getBatteryCapacity()+", "+
                "current: "+getSensingAverageCurrent()+", "+
                "isCharging: "+(isCharging()? "true" : "false")+", "+
                "isFull: "+((isFull())? "true" : "false")+
                "timestamp: "+System.nanoTime()+"}";
    }

    protected void enableActiveLogging(){
        OffloadingLogger.log(NAME_TAG, "Enabling active logging.");
        activeLogging = true;
    }

    protected void disableActiveLogging(){
        activeLogging = false;
    }
}
