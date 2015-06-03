package pt.ulisboa.tecnico.cycleourcity.scout.offloading;

import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.StageProfiler;

/**
 * Created by rodrigo.jm.lourenco on 01/06/2015.
 */
public class OffloadingDecisionEngine {

    //Debugging
    public static boolean VERBOSE = true;
    private final String LOG_TAG = AdaptiveOffloadingManager.LOG_TAG;
    private final String NAME_TAG = this.getClass().getSimpleName();

    //
    private boolean isMonitoring = false;
    public static int OFFLOAD_ATTEMPT_DEFAULT_INTERVAL  = 15*1000;
    private static int ATTEMPT_OFFLOAD_INTERVAL = OFFLOAD_ATTEMPT_DEFAULT_INTERVAL;

    private ScoutProfiler appProfiler;
    private Future executorHandler;
    private ScheduledExecutorService engineExecutor;
    private AdaptiveOffloadingManager.OffloadingObserver observer;

    //Apathy
    private float apathy;
    public static final float RECOMMENDED_APATHY    = (float) .5;
    public static final float NO_APATHY             = (float)  0;
    public static final float HYPER_APATHY          = (float) 20;
    public static final float REDUCED_APATHY        = (float).25;

    private static OffloadingDecisionEngine ENGINE = null;
    private OffloadingDecisionEngine(final ScoutProfiler appProfiler){

        this.appProfiler = appProfiler;
        engineExecutor = Executors.newSingleThreadScheduledExecutor();

        apathy = RECOMMENDED_APATHY;
    }

    /**
     * Redefines the apathy level. The bigger the apathy, the lazier the OffloadingDecisionEngine
     * becomes by postponing offloading.
     * @param apathy
     */
    protected void setApathy(float apathy){
        this.apathy = apathy;
    }

    public void startMonitoring(){

        if(!isMonitoring) {
            isMonitoring = true;

            executorHandler = engineExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {

                    boolean offload;
                    offload = isTimeToOffload(
                            appProfiler.getBatteryCapacity(),
                            appProfiler.getSensingAverageCurrent());

                    if(offload){
                        if(VERBOSE) Log.d(LOG_TAG, NAME_TAG+" has deemed it opportunistic to perform computation offloading.");
                        observer.notifyTimeOffloadOpportunity();
                    }else
                        if(VERBOSE) Log.d(LOG_TAG, NAME_TAG+" has deemed computation offloading unnecessary.");

                }
            }, ATTEMPT_OFFLOAD_INTERVAL, ATTEMPT_OFFLOAD_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }

    public void stopMonitoring(){
        if(isMonitoring) {
            //engineExecutor.shutdownNow();
            executorHandler.cancel(true);
            isMonitoring = false;
        }
    }

    protected void destroy(){
        engineExecutor.shutdownNow();
    }

    protected static OffloadingDecisionEngine getInstance(ScoutProfiler appProfiler,
                                                          AdaptiveOffloadingManager.OffloadingObserver observer){
        if(ENGINE==null){
            synchronized (OffloadingDecisionEngine.class){
                if(ENGINE==null) {
                    ENGINE = new OffloadingDecisionEngine(appProfiler);
                    ENGINE.setObserver(observer);
                }
            }
        }

        return ENGINE;
    }

    private void setObserver(AdaptiveOffloadingManager.OffloadingObserver observer){
        this.observer = observer;
    }


    public boolean isTimeToOffload(int capacity, float energy){
        return isTimeToOffload(capacity, energy, apathy);
    }

    public boolean isTimeToOffload(int capacity, float energy, float priority){
        float capacityPercentage = (float)capacity/100;
        return (1-capacityPercentage)*energy >= capacityPercentage*priority;
    }
}