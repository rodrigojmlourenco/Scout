package pt.ulisboa.tecnico.cycleourcity.scout.offloading;

import android.util.Log;

import com.ideaimpl.patterns.pipeline.Stage;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.AdaptivePipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.AdaptiveOffloadingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.InvalidOffloadingStageException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.NothingToRetrieveException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.TaggingStageMissingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.exceptions.NothingToOffloadException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.Rule;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.exceptions.UnableToEnforceRuleException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.ConfigurationTaggingStage;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.OffloadingWrapperStage;

public class PartitionEngine {

    protected boolean VERBOSE = true;
    private final String LOG_TAG = "PartitionEngine";

    private final OffloadTracker offloadTracker;

    private List<AdaptivePipelineTracker> validatedPipelines;

    private Rule enforcedRule = null;

    public PartitionEngine(){
        validatedPipelines = new ArrayList<>();
        offloadTracker = new OffloadTracker();
    }

    /**
     * Checks if a given pipeline configuration has been designed to support adaptive offloading.
     * <br>
     * This means that each of the pipeline's main stages as been wrapped by a ProfilingStageWrapper,
     * and that the pipeline contemplates a AdaptiveOffloadingTaggingStage, as one of his final
     * stages.
     * <br>
     * If this method is not performed over a pipeline, that pipeline will not be taken into account
     * when performing adaptive offloading.
     * <br>
     * Additionally this methods updates the OffloadingTracker
     *
     * @see OffloadTracker
     *
     * @param pipeline the PipelineConfiguration to be evaluated
     * @throws AdaptiveOffloadingException if the pipeline has not been designed to support adaptive offloading.
     */
    public void validatePipeline(AdaptivePipeline pipeline)
            throws InvalidOffloadingStageException, TaggingStageMissingException {

        for(Stage stage : pipeline.getAdaptiveStages())
            if(!(stage instanceof OffloadingWrapperStage))
                throw new InvalidOffloadingStageException();

        ConfigurationTaggingStage taggingStage = null;
        for(Stage finalStage : pipeline.getFinalStages())
            if(finalStage instanceof ConfigurationTaggingStage) {
                taggingStage = (ConfigurationTaggingStage) finalStage;

                //Update the tagging stage
                taggingStage.setPipelineUID(pipeline.hashCode());
                taggingStage.setOffloadTracker(offloadTracker);
            }

        if(taggingStage==null)
            throw new TaggingStageMissingException();

        AdaptivePipelineTracker tracker = new AdaptivePipelineTracker(pipeline);
        this.validatedPipelines.add(tracker);

        if(enforcedRule!=null)tracker.updateWeights(
                enforcedRule.getTimeWeight(),
                enforcedRule.getMobileCostWeight(),
                enforcedRule.getTransmissionWeight());
    }

    /**
     * Updates the AdaptivePipelineTrackers weight according to the new rule
     * being enforced.
     *
     * @param rule New enforced rule
     */
    protected void updateEnforcedRule(Rule rule) {

        if(enforcedRule != null && enforcedRule.equals(rule)){
            if(VERBOSE) Log.d(LOG_TAG, "Skipping enforced rule update has the rule is the same.");
            return;
        }

        enforcedRule = rule;

        if(VERBOSE) Log.d(LOG_TAG, "Updating the enforced rule to '" + rule.getRuleName() + "'.");

        for(AdaptivePipelineTracker p : validatedPipelines)
            p.updateWeights(
                    enforcedRule.getTimeWeight(),
                    enforcedRule.getMobileCostWeight(),
                    enforcedRule.getTransmissionWeight());
    }



    /**
     * When executed this method trims each validated pipeline as to
     * employ the optimal configuration as enforced by the current Rule.
     */
    protected void optimizePipelines() throws UnableToEnforceRuleException {

        for (AdaptivePipelineTracker p : validatedPipelines) {
            int offloadIterations = p.computeIdealOffloadIterations();

            if(offloadIterations > 0){
                if(VERBOSE) Log.d(LOG_TAG, "Offloading "+offloadIterations+" stages in pipeline "+p);
                offloadStages(p, offloadIterations);
            }else if(offloadIterations < 0){
                if(VERBOSE) Log.d(LOG_TAG, "Retrieve "+Math.abs(offloadIterations)+" stages to the pipeline "+p);
                retrieveStages(p, Math.abs(offloadIterations));
            }else if(VERBOSE)
                Log.d(LOG_TAG, "The original configuration is ideal so nothing will be offloaded.");

        }
    }

    private void offloadStages(AdaptivePipelineTracker tracker, int iterations){
        for(int i=0; i < iterations; i++) {
            try {
                offloadTracker.markOffloadedStage(tracker.getPipeline(), tracker.offloadStage());
            } catch (NothingToOffloadException e) {
                if(VERBOSE) Log.e(LOG_TAG, "Nothing to offload in this pipeline");
            }
        }
    }

    private void retrieveStages(AdaptivePipelineTracker tracker, int iterations){
        for(int i=0; i < iterations; i++)
            try {
                tracker.retrieveStage();
                offloadTracker.unmarkOffloadedStage(tracker.getPipeline());
            } catch (NothingToRetrieveException e) {
                e.printStackTrace();
            }
    }
}
