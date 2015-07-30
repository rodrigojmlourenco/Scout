package pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages;

import android.content.Context;

import com.ideaimpl.patterns.pipeline.Stage;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.AdaptivePipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.AdaptivePipelineTracker;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.OffloadTracker;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.AdaptiveOffloadingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.InvalidOffloadingStageException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.TaggingStageMissingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.pipelines.StageProfiler;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.RuleSetManager;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.exceptions.InvalidRuleSetException;

public class InstantPartitionEngine {

    private final String LOG_TAG = "PartitionEngine";

    private final StageProfiler stagesModel;
    private final OffloadTracker offloadTracker;
    private final RuleSetManager ruleSetFramework;

    //Testing
    private float timeWeight = 1, mobileCostWeight = 0, transmissionWeight = 0;


    private List<AdaptivePipelineTracker> validatedPipelines;

    public InstantPartitionEngine(Context context) throws InvalidRuleSetException {

        validatedPipelines = new ArrayList<>();

        stagesModel = StageProfiler.getInstance();
        offloadTracker = new OffloadTracker();
        ruleSetFramework = new RuleSetManager(context);
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

        this.validatedPipelines.add(
                new AdaptivePipelineTracker(pipeline, timeWeight, mobileCostWeight, transmissionWeight));
    }

    public void testRunOffload(){



        for (AdaptivePipelineTracker p : validatedPipelines) {
            int offloadIterations = p.computeIdealOffloadIterations();

            if(offloadIterations > 0){
                offloadStages(p, offloadIterations);
            }else if(offloadIterations < 0){
                throw new UnsupportedOperationException();
            }
        }
    }

    private void offloadStages(AdaptivePipelineTracker tracker, int iterations){
        for(int i=0; i < iterations; i++)
            offloadTracker.markOffloadedStage(tracker.getPipeline(), tracker.offloadStage());

    }


    /*
    private void selectIdealConfiguration(AdaptivePipelineTracker pipeline){

        //For n pipeline stages there are n+1 possible configurations
        int possibleConfigsCount    = pipeline.getAdaptiveStages().size()+1;
        float[] configsTotalUtility = new float[possibleConfigsCount];
        float[] configsTimes        = new float[possibleConfigsCount];
        float[] configsData         = new float[possibleConfigsCount];

        //1. Compute the "ideal" value for the two metrics:
        //  - time (will always be zero)
        //  - data
        float   idealTime = 0,
                idealData;

        for(Stage s : pipeline.getAdaptiveStages()){
            Log.d(LOG_TAG, ((OffloadingWrapperStage) s).getIdentifier());
        }
    }
    */


}
