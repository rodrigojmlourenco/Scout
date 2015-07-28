package pt.ulisboa.tecnico.cycleourcity.scout.offloading;

import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.AdaptivePipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.AdaptiveOffloadingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.OverearlyOffloadException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.exceptions.NoAdaptivePipelineValidatedException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.exceptions.NothingToOffloadException;

/**
 * Created by rodrigo.jm.lourenco on 01/06/2015.
 */
@Deprecated
public class DecisionEngine {

    //Debugging
    public static boolean VERBOSE = true;
    public final String LOG_TAG = AdaptiveOffloadingManager.LOG_TAG;
    public final String NAME_TAG = this.getClass().getSimpleName();

    //
    private boolean isMonitoring = false;
    public static int OFFLOAD_ATTEMPT_DEFAULT_INTERVAL  = 15*1000;
    public final static int WAIT_TO_ATTEMPT_OFFLOAD = 1*60*1000; //1min
    private static int ATTEMPT_OFFLOAD_INTERVAL = OFFLOAD_ATTEMPT_DEFAULT_INTERVAL;

    private ScoutProfiler appProfiler;

    private Future executorHandler;
    private ScheduledExecutorService engineExecutor;
    //private AdaptiveOffloadingManager.OffloadingObserver observer;

    //Apathy
    private float apathy;
    public static final float RECOMMENDED_APATHY    = (float) .5;
    public static final float NO_APATHY             = (float)  0;
    public static final float HYPER_APATHY          = (float) 20;
    public static final float REDUCED_APATHY        = (float).25;

    //Statistics
    private int offloadingAttempts  = 0;
    private int performedOffloads   = 0;

    //
    private PartitionEngine partitionEngine;

    private static DecisionEngine ENGINE = null;

    private final OffloadTracker offloadingTracker;

    protected DecisionEngine(final ScoutProfiler appProfiler){

        this.appProfiler = appProfiler;
        engineExecutor = Executors.newSingleThreadScheduledExecutor();

        apathy = RECOMMENDED_APATHY;

        offloadingTracker  = new OffloadTracker();
        partitionEngine = new PartitionEngine(offloadingTracker);
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

                    int battery     = appProfiler.getBatteryCapacity();
                    long current    = appProfiler.getSensingAverageCurrent();

                    boolean offload = false;//isTimeToOffload(battery, EnergyProfiler.convertToAmpere(current));

                    if(offload){
                        if(VERBOSE) Log.d(LOG_TAG, NAME_TAG+" has deemed it opportunistic to perform computation offloading.");

                        OffloadingLogger.log(NAME_TAG, "Offloading Opportunity after "+offloadingAttempts+" attempts");
                        OffloadingLogger.log(NAME_TAG, dumpOffloadInfo(battery, current, true));
                        offloadingAttempts = 0;

                        performedOffloads++;

                        try {
                            partitionEngine.offloadMostExpensiveStage();
                        } catch (NoAdaptivePipelineValidatedException e) {
                            e.printStackTrace();
                        } catch (NothingToOffloadException e) {
                            e.printStackTrace();
                        } catch (OverearlyOffloadException e) {
                            e.printStackTrace();
                        }

                    }else {
                        if (VERBOSE) Log.d(LOG_TAG, NAME_TAG + " has deemed computation offloading unnecessary.");
                        OffloadingLogger.log(appProfiler.NAME_TAG, appProfiler.dumpInfo());
                        offloadingAttempts++;
                    }

                    /*
                    if(MockupBatteryProfiler.isActive())
                        MockupBatteryProfiler.decrementBattery();
                    */

                }
            }, WAIT_TO_ATTEMPT_OFFLOAD, ATTEMPT_OFFLOAD_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }

    public void stopMonitoring(){
        if(isMonitoring) {
            //engineExecutor.shutdownNow();
            executorHandler.cancel(true);
            isMonitoring = false;
        }
    }

    public OffloadTracker getOffloadingTracker(){ return offloadingTracker; }


    protected void destroy(){
        engineExecutor.shutdownNow();
    }

    /*
    protected static DecisionEngine getInstance(ScoutProfiler appProfiler,
                                                          AdaptiveOffloadingManager.OffloadingObserver observer){
        if(ENGINE==null){
            synchronized (DecisionEngine.class){
                if(ENGINE==null) {
                    ENGINE = new DecisionEngine(appProfiler);
                    ENGINE.setObserver(observer);
                }
            }
        }

        return ENGINE;
    }
    */

    private boolean isTimeToOffload(int capacity, float energy){
        return isTimeToOffload(capacity, energy, apathy);
    }

    private boolean isTimeToOffload(int capacity, float energy, float priority){
        float capacityPercentage = (float)capacity/100;
        return (1-capacityPercentage)*energy >= capacityPercentage*priority;
    }

    /*
     ************************************************************************
     * Decision Engine Statistics                                           *
     ************************************************************************
     */

    public int getOffloadingAttempts(){
        return offloadingAttempts;
    }

    public int getPerformedOffloads(){ return  performedOffloads; }

    public String dumpInfo(){
        return "{name: \""+NAME_TAG+"\","+
                "offloads: "+getPerformedOffloads()+", "+
                "attemptsSinceLastOffload: "+getOffloadingAttempts()+", "+
                "timestamp: "+System.nanoTime()+"}";
    }

    public String dumpOffloadInfo(int battery, long current, boolean offload){
        return "{name: \""+NAME_TAG+"\", "+
                "offloadInfo: {"+
                "battery: "+battery+", "+
                "current: "+current+", "+
                "isOpportunity: "+(offload ? "true" : "false")+", "+
                "timestamp: "+System.nanoTime()+"}"+
                "}";
    }


    /*
    ************************************************************************
    * Pipeline Partition Engine                                            *
    ************************************************************************
    */

    protected void setTotalUtilityWeights(float energyWeight, float dataWeight){
        partitionEngine.setEnergyWeight(energyWeight);
        partitionEngine.setDataWeight(dataWeight);
    }

    protected float getEnergyWeight(){
        return partitionEngine.getEnergyWeight();
    }

    protected float getDataWeight(){
        return partitionEngine.getDataWeight();
    }


    protected void validatePipeline(AdaptivePipeline pipeline) throws AdaptiveOffloadingException {
        partitionEngine.validatePipeline(pipeline);
    }

    protected void clearState(){
        partitionEngine.clearState();
    }

    /*
     ************************************************************************
     * Testing                                                              *
     ************************************************************************
     */
    protected void offloadWorstStage()
            throws NothingToOffloadException, NoAdaptivePipelineValidatedException, OverearlyOffloadException {

        partitionEngine.offloadMostExpensiveStage();
    }
}