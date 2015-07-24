package pt.ulisboa.tecnico.cycleourcity.scout.offloading;

import android.util.Log;

import com.ideaimpl.patterns.pipeline.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.AdaptivePipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.AdaptiveOffloadingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.InvalidOffloadingStageException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.OverearlyOffloadException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.TaggingStageMissingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.exceptions.NoAdaptivePipelineValidatedException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.exceptions.NothingToOffloadException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.ConfigurationTaggingStage;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.OffloadingStageWrapper;


public class PartitionEngine {

    //TODO: remover
    private final boolean VERBOSE = true;

    private final String LOG_TAG = AdaptiveOffloadingManager.LOG_TAG;
    private final String NAME_TAG = getClass().getSimpleName();

    private ArrayList<AdaptivePipeline> validatedPipelines;

    private final OffloadTracker offloadTracker;

    protected PartitionEngine(OffloadTracker tracker){
        validatedPipelines = new ArrayList<>();
        offloadTracker = tracker;
    }

    /**
     * Checks if a given pipeline configuration has been designed to support adaptive offloading.
     * This means that each of the pipeline's main stages as been wrapped by a ProfilingStageWrapper,
     * and that the pipeline contemplates a AdaptiveOffloadingTaggingStage, as one of his final
     * stages.
     * <br>
     * If this method is not performed over a pipeline, that pipeline will not be taken into account
     * when performing adaptive offloading.
     *
     * @param pipeline the PipelineConfiguration to be evaluated
     * @throws AdaptiveOffloadingException if the pipeline has not been designed to support adaptive offloading.
     */
    public void validatePipeline(AdaptivePipeline pipeline)
            throws InvalidOffloadingStageException, TaggingStageMissingException {

        for(Stage stage : pipeline.getAdaptiveStages())
            if(!(stage instanceof OffloadingStageWrapper))
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

        this.validatedPipelines.add(pipeline);
    }


    public Stage offloadMostExpensiveStage() throws NoAdaptivePipelineValidatedException, NothingToOffloadException, OverearlyOffloadException {

        if(validatedPipelines.isEmpty()) throw new NoAdaptivePipelineValidatedException();

        if(VERBOSE)
            OffloadingLogger.log(this.getClass().getSimpleName(),
                    "[1] Initiating offloading process... Offloading the last stage of one of " + validatedPipelines.size() + "pipelines");

        //PHASE 1 - Preparation
        List<Long>  executionTimesByStage = new ArrayList<>(),
                    generatedDataByStage = new ArrayList<>();

        List<OffloadingStageWrapper> offloadingStageOptions = new ArrayList<>();

        ////Fetch the costs for each stage
        OffloadingStageWrapper auxStage;
        for(AdaptivePipeline p : validatedPipelines){


            auxStage = (OffloadingStageWrapper) p.getLastAdaptiveStage();

            if(auxStage==null){
                if(VERBOSE) OffloadingLogger.log(this.getClass().getSimpleName(), p.getClass().getSimpleName() + " has no more stages");
                //validatedPipelines.remove(p);
                //continue;
                Log.e("AdaptiveOffloading", "TESTING if continues...");
            }else {

                try {
                    offloadingStageOptions.add(auxStage);
                    executionTimesByStage.add(auxStage.getAverageRunningTime());
                    generatedDataByStage.add(auxStage.getAverageGeneratedDataSize());
                }catch (ArithmeticException e){
                    throw new OverearlyOffloadException();
                }
            }
        }

        if(validatedPipelines.isEmpty()) {
            Log.e("AdaptiveOffloading", "All pipelines were offloaded.");
            throw new NothingToOffloadException();
        }


        ////Aux variables:
        long    totalExecutionTime  = 0,
                totalGeneratedData  = 0,
                optimalGeneratedData= 0,
                optimalExecutionTime= 0;
        try {
            totalExecutionTime = getTotalExecutionTime();
            totalGeneratedData = getTotalGeneratedData();
            optimalGeneratedData = getOptimalGeneratedData(generatedDataByStage);
            optimalExecutionTime = getOptimalExecutionTime(executionTimesByStage);
        }catch (ArithmeticException e){
            throw new OverearlyOffloadException();
        }

        StageCostComputer decider =
                new MultiCriteriaStageCostComputer(totalExecutionTime,
                                                    optimalGeneratedData, optimalExecutionTime);

        //PHASE 2 - Decision
        int i, worst;
        OffloadingStageWrapper
                auxLastStage,
                mostExpensiveStage = (OffloadingStageWrapper) validatedPipelines.get(0).getLastAdaptiveStage();

        for(i=0, worst=0; i < validatedPipelines.size(); i++){

            auxLastStage = (OffloadingStageWrapper) validatedPipelines.get(i).getLastAdaptiveStage();

            if(!decider.isMoreExpensive(mostExpensiveStage, auxLastStage)){
                mostExpensiveStage = auxLastStage;
                worst = i;
            }
        }

        //Logging
        if(VERBOSE)
            Log.d(LOG_TAG, "The most expensive stage is '"+mostExpensiveStage.getStageClass().getSimpleName()+
         "' which belong to the "+validatedPipelines.get(worst).getClass().getSimpleName());

        OffloadingLogger.log(NAME_TAG, dumpInfo(mostExpensiveStage, offloadingStageOptions));

        //PHASE 3 - Offloading
        AdaptivePipeline p  = validatedPipelines.get(worst);
        Stage offloadedStage= p.removeStage();

        offloadTracker.markOffloadedStage(p, (OffloadingStageWrapper) offloadedStage);

        //PHASE 4 - Adjust the pipelines
        if(p.getAdaptiveStages().isEmpty())
            validatedPipelines.remove(p);

        return offloadedStage;
    }


    /*
     ************************************************************************
     * Multi-Criteria Decision Theory                                       *
     ************************************************************************
     */
    private long getTotalExecutionTime(){
        long total = 0;

        for(AdaptivePipeline p : validatedPipelines){
            for(Stage stage : p.getAdaptiveStages())
                total += ((OffloadingStageWrapper)stage).getAverageRunningTime();
        }

        return total;
    }

    private long getTotalGeneratedData(){
        long total = 0;

        for(AdaptivePipeline p : validatedPipelines)
            total += ((OffloadingStageWrapper) p.getLastAdaptiveStage()).getAverageGeneratedDataSize();

        return total;
    }

    private long getOptimalGeneratedData(List<Long> dataSizes){

        long totalGeneratedData = getTotalGeneratedData();
        List<Long> newSizes = new ArrayList<>();

        for(Long size : dataSizes)
            newSizes.add(totalGeneratedData - size);

        return Collections.min(newSizes);
    }

    private long getOptimalExecutionTime(List<Long> executionTimes){

        long totalTime = getTotalExecutionTime();
        List<Long> newTimes = new ArrayList<>();

        for(Long time : executionTimes)
            newTimes.add(totalTime-time);

        return Collections.min(newTimes);
    }

    public static abstract class StageCostComputer {

        public final static float TIME_WEIGHT = (float)1;
        public final static float DATA_WEIGHT = (float)0;

        public abstract float computeCost(OffloadingStageWrapper stage);

        public boolean isMoreExpensive(OffloadingStageWrapper baseStage, OffloadingStageWrapper stage){

            float cost1, cost2;

            cost1 = computeCost(baseStage);
            cost2 = computeCost(stage);

            return cost1 <= cost2;
        }
    }

    /**
     * @see <a href="http://www.utdallas.edu/~cxl137330/courses/spring14/AdvRTS/protected/slides/43.pdf">SociableSense</a>
     */
    public static class MultiCriteriaStageCostComputer extends StageCostComputer{

        private final long
                totalExecutionTime,
                optimalGeneratedData,
                optimalExecutionTime;


        protected MultiCriteriaStageCostComputer(long totalExecutionTime,
                                                 long optimalGeneratedData, long optimalExecutionTime){

            super();

            this.totalExecutionTime = totalExecutionTime;
            this.optimalGeneratedData = optimalGeneratedData;
            this.optimalExecutionTime = optimalExecutionTime;

        }


        private float timeUtilityFunction(long time){
            long newTotalTime = totalExecutionTime - time;
            return (float)(optimalExecutionTime-newTotalTime)/newTotalTime;
        }

        private float dataUtilityFunction(long data){
            return (float)(optimalGeneratedData-data)/data;
        }


        @Override
        public float computeCost(OffloadingStageWrapper stage) {
            return  TIME_WEIGHT*timeUtilityFunction(stage.getAverageRunningTime()) +
                    DATA_WEIGHT*dataUtilityFunction(stage.getAverageGeneratedDataSize());

        }
    }

    protected void clearState(){
        this.validatedPipelines.clear();
    }

    /*
     ****************************************************************************
     * Information Logging                                                      *
     ****************************************************************************
     */

    private String dumpInfo(OffloadingStageWrapper choice, List<OffloadingStageWrapper> stages){
        return "{ name: \""+NAME_TAG+"\", "+
                "chosen: "+choice.dumpInfo()+", "+
                "options: "+dumpOffloadingOptionsInfo(stages)+"}";
    }

    private String dumpOffloadingOptionsInfo(List<OffloadingStageWrapper> stages){
        String info = "[ ";

        int i=1, options = stages.size();
        for(OffloadingStageWrapper s : stages)
            info += s.dumpInfo() + (i++ < options ? ", " : "]");

        return info;
    }

    private String dumpOffloadingChoiceInfo(OffloadingStageWrapper stage){
        return stage.dumpInfo();
    }
}

