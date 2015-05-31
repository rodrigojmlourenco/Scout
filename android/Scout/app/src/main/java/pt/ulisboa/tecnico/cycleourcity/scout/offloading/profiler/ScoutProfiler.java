package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.MobileSensing;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.resources.EnergyProfiler;

/**
 * Created by rodrigo.jm.lourenco on 31/05/2015.
 */
public class ScoutProfiler {

    private Object lockState = new Object();
    private MobileSensing.MobileSensingState sensingState;
    private EnergyProfiler iddleEProf, sensingEProf;

    public ScoutProfiler(Context context, MobileSensing.MobileSensingState sensingState){

        this.sensingState = sensingState;

        iddleEProf = new EnergyProfiler.IdleEnergyProfiler(
                (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE),
                context.getSharedPreferences(EnergyProfiler.PREFS_NAME, Context.MODE_PRIVATE));

        sensingEProf = new EnergyProfiler.SensingEnergyProfiler(
                (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE),
                context.getSharedPreferences(EnergyProfiler.PREFS_NAME, Context.MODE_PRIVATE));

    }

    private boolean isProfiling = false;
    private int profilingRate = 50; //2s

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public void setProfileScheduleRate(int seconds){
        profilingRate = seconds * 1000;
    }

    public boolean isProfiling(){
        synchronized (lockState) {
            return isProfiling;
        }
    }

    public void startProfiling(){
        //executorService.scheduleAtFixedRate(new Runnable() {
        executorService.scheduleWithFixedDelay(new Runnable() {
        @Override
            public void run() {

                synchronized (lockState) {
                    isProfiling = true;
                }

                if(sensingState.isSensing())
                    sensingEProf.profile();
                else
                    iddleEProf.profile();

            }
        }, 0, profilingRate, TimeUnit.MILLISECONDS);
    }

    public void stopProfiling(){
        executorService.shutdownNow();
        synchronized (lockState) {
            isProfiling = true;
        }
    }
}
