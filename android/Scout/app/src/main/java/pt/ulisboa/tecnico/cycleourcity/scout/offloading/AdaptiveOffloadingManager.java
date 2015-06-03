package pt.ulisboa.tecnico.cycleourcity.scout.offloading;

import android.content.Context;

import java.text.DateFormat;
import java.util.Date;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.PipelineConfiguration;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.AdaptiveOffloadingException;

/**
 * Created by rodrigo.jm.lourenco on 02/06/2015.
 */
public class AdaptiveOffloadingManager {

    protected final static String LOG_TAG = "AdaptiveOffloading";

    private final ScoutProfiler applicationProfiler;
    private final OffloadingDecisionEngine decisionEngine;
    private final PipelinePartitionEngine partitionEngine;


    private static AdaptiveOffloadingManager OFFLOADING_MANAGER = null;

    private AdaptiveOffloadingManager(Context context){
        applicationProfiler = ScoutProfiler.getInstance(context);
        partitionEngine     = PipelinePartitionEngine.getInstance();
        decisionEngine      = OffloadingDecisionEngine.getInstance(applicationProfiler,this.decisionEngineObserver);
    }

    public static AdaptiveOffloadingManager getInstance(Context context){
        if(OFFLOADING_MANAGER == null)
            synchronized (AdaptiveOffloadingManager.class){
                if(OFFLOADING_MANAGER == null)
                    OFFLOADING_MANAGER = new AdaptiveOffloadingManager(context);
            }
        return OFFLOADING_MANAGER;
    }

    public void onDestroy(){
        decisionEngine.destroy();
        applicationProfiler.destroy();
    }

    /*
     ************************************************************************
     * Profiling Functions                                                  *
     ************************************************************************
     */
    public void startProfiling(int rateMillis){
        try {
            applicationProfiler.setProfilingRate(rateMillis);
            applicationProfiler.startProfiling();
        } catch (AdaptiveOffloadingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the application's profiler is enabled
     * @return True if the profiler enabled, false otherwise.
     */
    public boolean isProfiling(){
        return applicationProfiler.isProfiling();
    }


    public void startProfiling(){

        try {
            OffloadingLogger.log(getClass().getSimpleName(),
                    "[BEGIN SESSION "+ DateFormat.getTimeInstance().format(new Date())+"]");

            applicationProfiler.setProfilingRate(ScoutProfiler.DEFAULT_PROFILING_RATE);
            applicationProfiler.startProfiling();
            decisionEngine.startMonitoring();
        } catch (AdaptiveOffloadingException e) {
            e.printStackTrace();
        }
    }


    public void stopProfiling(){
        applicationProfiler.stopProfiling();
        decisionEngine.stopMonitoring();

        //Logging
        OffloadingLogger.log(getClass().getSimpleName(), "[TERMINATING SESSION]");
        OffloadingLogger.log(decisionEngine.NAME_TAG,
                "{name: \"" + decisionEngine.NAME_TAG + "\", " +
                        " offloads: " + decisionEngine.getPerformedOffloads() + ", " +
                        " attemptsSinceOffload: " + decisionEngine.getOffloadingAttempts() + "}");
    }


    public int getRemainingBattery(){
        return applicationProfiler.getBatteryCapacity();
    }


    public long getAverageCurrent(){
        return applicationProfiler.getSensingAverageCurrent();
    }


    /*
     ************************************************************************
     * Pipeline Partition Engine                                            *
     ************************************************************************
     */


    public void validatePipeline(PipelineConfiguration configuration) throws AdaptiveOffloadingException {
        partitionEngine.validatePipeline(configuration);
    }

    /*
     ************************************************************************
     * Decision Engine                                                      *
     ************************************************************************
     */
    protected static interface OffloadingObserver{
        public void notifyTimeOffloadOpportunity();
    }

    private OffloadingObserver decisionEngineObserver = new OffloadingObserver() {
        @Override
        public void notifyTimeOffloadOpportunity() {
            //TODO: enable
            //partitionEngine.offloadMostExpensiveStage();
        }
    };

    /**
     * Redefines the apathy level. The bigger the apathy, the lazier the OffloadingDecisionEngine
     * becomes, by postponing offloading.
     * @param apathy
     */
    public void setDecisionEngineApathy(float apathy){
        this.decisionEngine.setApathy(apathy);
    }

    /*
     ************************************************************************
     * Logging                                                              *
     ************************************************************************
     */
    public void exportOffloadingLog(){
        OffloadingLogger.exportLog();
    }

}
