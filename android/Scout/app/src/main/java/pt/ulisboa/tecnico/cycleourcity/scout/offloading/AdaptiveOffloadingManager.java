package pt.ulisboa.tecnico.cycleourcity.scout.offloading;

import android.content.Context;

import java.text.DateFormat;
import java.util.Date;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.AdaptivePipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.AdaptiveOffloadingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.OverearlyOffloadException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.device.DeviceStateProfiler;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.exceptions.NoAdaptivePipelineValidatedException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.exceptions.NothingToOffloadException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.exceptions.InvalidRuleSetException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.InstantPartitionEngine;

public class AdaptiveOffloadingManager {

    protected final static String LOG_TAG = "AdaptiveOffloading";
    private final String NAME_TAG = this.getClass().getSimpleName();


    private final InstantPartitionEngine partitionEngine;
    private final DeviceStateProfiler deviceState;

    private boolean isProfilingEnabled = false;

    private static AdaptiveOffloadingManager OFFLOADING_MANAGER = null;

    private AdaptiveOffloadingManager(Context context) throws InvalidRuleSetException {
        deviceState = new DeviceStateProfiler(context);
        partitionEngine = new InstantPartitionEngine(context);
    }

    public static AdaptiveOffloadingManager getInstance(Context context)
            throws InvalidRuleSetException {

        if(OFFLOADING_MANAGER == null)
            synchronized (AdaptiveOffloadingManager.class){
                if(OFFLOADING_MANAGER == null)
                    OFFLOADING_MANAGER = new AdaptiveOffloadingManager(context);
            }

        return OFFLOADING_MANAGER;
    }

    public void onDestroy(){
        deviceState.teardown();
    }

    /*
     ************************************************************************
     * Profiling Functions                                                  *
     ************************************************************************
     */
    @Deprecated
    public void startProfiling(int rateMillis){
        /*
        try {
            applicationProfiler.setProfilingRate(rateMillis);
            applicationProfiler.startProfiling();
        } catch (AdaptiveOffloadingException e) {
            e.printStackTrace();
        }
        */
    }

    /**
     * Checks if the application's profiler is enabled
     * @return True if the profiler enabled, false otherwise.
     */
    @Deprecated
    public boolean isProfiling(){
        //return applicationProfiler.isProfiling();
        return false;
    }


    @Deprecated
    public void startProfiling(){

        /*
        try {
            OffloadingLogger.log(getClass().getSimpleName(),
                    "[BEGIN SESSION "+ DateFormat.getTimeInstance().format(new Date())+"]");

            applicationProfiler.setProfilingRate(ScoutProfiler.DEFAULT_PROFILING_RATE);
            applicationProfiler.startProfiling();
            decisionEngine.startMonitoring();
        } catch (AdaptiveOffloadingException e) {
            e.printStackTrace();
        }
        */
    }


    @Deprecated
    public void stopProfiling(){
        //applicationProfiler.stopProfiling();
        //decisionEngine.stopMonitoring();

        //Logging
        OffloadingLogger.log(getClass().getSimpleName(), "[TERMINATING SESSION]");
        /*OffloadingLogger.log(decisionEngine.NAME_TAG,
                "{name: \"" + decisionEngine.NAME_TAG + "\", " +
                        " offloads: " + decisionEngine.getPerformedOffloads() + ", " +
                        " attemptsSinceOffload: " + decisionEngine.getOffloadingAttempts() + "}");
        */
    }


    /*
     ************************************************************************
     * Partition Engine                                                     *
     ************************************************************************
     */
    public void validatePipeline(AdaptivePipeline pipeline) throws AdaptiveOffloadingException {
        partitionEngine.validatePipeline(pipeline);
    }


    /*
     ************************************************************************
     * Logging                                                              *
     ************************************************************************
     */
    public void exportOffloadingLog(){
        OffloadingLogger.exportLog();
    }

    /*
     ************************************************************************
     * Internal State                                                       *
     ************************************************************************
     */
    public boolean isProfilingEnabled(){ return isProfilingEnabled; }

    public void enableProfiling(){ isProfilingEnabled = true; }

    public void disableProfiling(){ isProfilingEnabled = false; }

    /*
     ************************************************************************
     * Testing TODO remover                                                 *
     ************************************************************************
     */
    public void forceOffloading()
            throws NothingToOffloadException, NoAdaptivePipelineValidatedException, OverearlyOffloadException {

        partitionEngine.testRunOffload();
    }

}
