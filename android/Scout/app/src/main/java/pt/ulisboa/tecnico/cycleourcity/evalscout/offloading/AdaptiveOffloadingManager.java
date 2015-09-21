package pt.ulisboa.tecnico.cycleourcity.evalscout.offloading;

import android.content.Context;
import android.util.Log;

import java.util.Observable;
import java.util.Observer;

import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.exceptions.OverearlyOffloadException;
import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.profiling.exceptions.NothingToOffloadException;
import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.ruleset.exceptions.InvalidRuleSetException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.AdaptivePipeline;
import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.exceptions.AdaptiveOffloadingException;
import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.profiling.device.DeviceStateProfiler;
import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.profiling.exceptions.NoAdaptivePipelineValidatedException;
import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.profiling.pipelines.StageProfiler;
import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.ruleset.Rule;
import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.ruleset.RuleSetManager;
import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.ruleset.exceptions.UnableToEnforceRuleException;

public class AdaptiveOffloadingManager implements Observer{

    protected final static String LOG_TAG = "AdaptiveOffloading";
    private static final boolean VERBOSE = true;
    private final String NAME_TAG = this.getClass().getSimpleName();



    private final DeviceStateProfiler   deviceState;
    private final PartitionEngine       partitionEngine;
    private final RuleSetManager ruleSetFrameWork;
    private final StageProfiler         stageModel;

    private boolean isProfilingEnabled = true;

    private static AdaptiveOffloadingManager OFFLOADING_MANAGER = null;

    private AdaptiveOffloadingManager(Context context) throws InvalidRuleSetException {

        stageModel      = StageProfiler.getInstance();
        deviceState     = new DeviceStateProfiler(context);
        partitionEngine = new PartitionEngine();
        ruleSetFrameWork= new RuleSetManager(context, deviceState);

        ruleSetFrameWork.addObserver(this);
        partitionEngine.updateEnforcedRule(ruleSetFrameWork.getEnforcedRule());
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
     * Partition Engine                                                     *
     ************************************************************************
     */
    public void resetPartitionEngine() { partitionEngine.teardown(); }

    public void validatePipeline(AdaptivePipeline pipeline) throws AdaptiveOffloadingException {
        if(offloadingEnabled) partitionEngine.validatePipeline(pipeline);
    }

    public void optimizePipelines(){

        if(!offloadingEnabled) {
            if (VERBOSE)
                Log.d(LOG_TAG, "Since the offloading is not enabled, the pipelines will not be optimized");
            return;
        }

        if(!stageModel.hasModel()){
            if(VERBOSE) Log.d(LOG_TAG, "No offloading will be performed "+
                    "has these configuration has not yet been modelled.");
            return;
        }

        try {
            partitionEngine.optimizePipelines();
        } catch (UnableToEnforceRuleException e) {
            e.printStackTrace();
        }
    }

    /*
     ********************************************************************
     * RuleSetFramework Observer                                        *
     ********************************************************************
     */
    @Override
    public void update(Observable observable, Object data) {

        if(!stageModel.hasModel()){
            if(VERBOSE) Log.d(LOG_TAG, "No offloading will be performed "+
                    "has these configuration has not yet been modelled.");
            return;
        }

        Rule rule = (Rule) data;
        partitionEngine.updateEnforcedRule(rule);

        try {
            partitionEngine.optimizePipelines();
        } catch (UnableToEnforceRuleException e) {
            if(VERBOSE)Log.w(LOG_TAG, e.getMessage());
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
    private boolean offloadingEnabled = false;

    public boolean isProfilingEnabled(){ return isProfilingEnabled; }


    public boolean isOffloadingEnabled(){
        return offloadingEnabled;
    }

    public void toggleOffloading(boolean isEnabled){
        offloadingEnabled = isEnabled;
    }

    public boolean isStageModelComplete(){
        return stageModel.hasModel();
    }


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

    public void forceObserverReaction(){
        ruleSetFrameWork.enforceRule(null);
    }

    private boolean isMockup = false;

    public void forceMockUp() {

        if(VERBOSE) Log.d(LOG_TAG, "Running in mockup mode, device state will be simulated.");

        isMockup = true;

        deviceState.forceMockUp();
    }

    public void forceUpdateBatteryLevel(int level){
        if(!isMockup)return;

        deviceState.forceBatteryUpdate(level);
    }

    public void forceUpdateNetworkType(int net) {
        //TODO
    }
}
