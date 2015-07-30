package pt.ulisboa.tecnico.cycleourcity.scout.offloading;

import android.content.Context;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.AdaptivePipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.AdaptiveOffloadingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.OverearlyOffloadException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.device.DeviceStateProfiler;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.exceptions.NoAdaptivePipelineValidatedException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.exceptions.NothingToOffloadException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.RuleSetManager;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.exceptions.InvalidRuleSetException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.exceptions.UnableToEnforceRuleException;

public class AdaptiveOffloadingManager {

    protected final static String LOG_TAG = "AdaptiveOffloading";
    private final String NAME_TAG = this.getClass().getSimpleName();



    private final DeviceStateProfiler       deviceState;
    private final PartitionEngine partitionEngine;
    private final RuleSetManager            ruleSetFrameWork;


    private boolean isProfilingEnabled = false;

    private static AdaptiveOffloadingManager OFFLOADING_MANAGER = null;

    private AdaptiveOffloadingManager(Context context) throws InvalidRuleSetException {
        deviceState     = new DeviceStateProfiler(context);
        partitionEngine = new PartitionEngine();
        ruleSetFrameWork= new RuleSetManager(context);

        partitionEngine.updateEnforcedRule(
                ruleSetFrameWork.selectRuleToEnforce(deviceState.getDeviceState()));

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








    /*
     ************************************************************************
     * Partition Engine                                                     *
     ************************************************************************
     */
    public void validatePipeline(AdaptivePipeline pipeline) throws AdaptiveOffloadingException {
        partitionEngine.validatePipeline(pipeline);
    }

    public void optimizePipelines(){
        try {
            partitionEngine.optimizePipelines();
        } catch (UnableToEnforceRuleException e) {
            e.printStackTrace();
        }
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

        try {
            partitionEngine.optimizePipelines();
        } catch (UnableToEnforceRuleException e) {
            e.printStackTrace();
        }
    }

}
