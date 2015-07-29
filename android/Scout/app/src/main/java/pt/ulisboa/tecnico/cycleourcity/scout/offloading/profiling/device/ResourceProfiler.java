package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.device;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by rodrigo.jm.lourenco on 29/05/2015.
 */
public abstract class ResourceProfiler {

    private boolean isProfiling = false;
    private int profilingRate = 2000; //2s

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public void setProfileScheduleRate(int seconds){
        profilingRate = seconds * 1000;
    }

    public boolean isProfiling(){
        return  isProfiling;
    }

    public void startProfiling(){
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                profile();
                isProfiling = true;
            }
        }, 0, profilingRate, TimeUnit.MILLISECONDS);
    }

    public void stopProfiling(){
        executorService.shutdownNow();
        isProfiling = false;
    }

    protected abstract void profile();

    public abstract String dumpInfo();
}
