package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.resources;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.TrafficStats;

/**
 * Created by rodrigo.jm.lourenco on 29/05/2015.
 */
public class TrafficStatsProfiler extends ResourceProfiler {

    private final int applicationID;
    private final TrafficStats trafficStats;
    private final SharedPreferences settings;
    private final SharedPreferences.Editor settingsEditor;

    private final static String PREFS_NAME = "TrafficStats";

    //Properties
    private long totalSentBytes;
    private long lastSentBytes;

    public TrafficStatsProfiler(Context context, int appUID){
        applicationID = appUID;
        trafficStats = new TrafficStats();
        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        settingsEditor = settings.edit();

        totalSentBytes = trafficStats.getUidTxBytes(applicationID);
    }

    private void storeInfo(String key, String value){
        settingsEditor.putString(key, value);
        settingsEditor.commit();
    }

    @Override
    public void profile() {

        long aux = trafficStats.getUidTxBytes(applicationID);

        lastSentBytes = aux - lastSentBytes;
        totalSentBytes = aux;
    }

    @Override
    public String dumpInfo() {
        String info = "["+getClass().getSimpleName()+"]\n"+
                "\tTotal Bytes Transmited:"+totalSentBytes+"\n"+
                "\tBytes Sent since last profile: "+lastSentBytes;

        return info;
    }
}
