package pt.ulisboa.tecnico.cycleourcity.scout.offloading;

import android.util.Log;

import com.ideaimpl.patterns.pipeline.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.PipelineConfiguration;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.AdaptiveOffloadingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.InvalidOffloadingStageException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.TaggingStageMissingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.AdaptiveOffloadingTaggingStage;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.ProfilingStageWrapper;

/**
 * Created by rodrigo.jm.lourenco on 01/06/2015.
 */
public class PipelinePartitionEngine {

    private final String LOG_TAG = getClass().getSimpleName();

    private static PipelinePartitionEngine PARTITIONER = null;

    private long totalExecutionTime;

    private ArrayList<PipelineConfiguration> validatedPipelines;
    private PipelinePartitionEngine(){
        validatedPipelines = new ArrayList<>();
    }

    protected static PipelinePartitionEngine getInstance(){
        if(PARTITIONER==null)
            synchronized (PipelinePartitionEngine.class){
                if(PARTITIONER == null)
                    PARTITIONER = new PipelinePartitionEngine();
            }
        return PARTITIONER;
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
    public void validatePipeline(PipelineConfiguration pipeline)
            throws AdaptiveOffloadingException{

        for(Stage stage : pipeline.getStages())
            if(!(stage instanceof ProfilingStageWrapper))
                throw new InvalidOffloadingStageException();

        boolean containsTaggingStage = false;
        for(Stage finalStage : pipeline.getFinalStages())
            if(finalStage instanceof AdaptiveOffloadingTaggingStage)
                containsTaggingStage = true;

        if(!containsTaggingStage)
            throw new TaggingStageMissingException();

        this.validatedPipelines.add(pipeline);
    }


    public void offloadMostExpensiveStage(){

        Log.d(LOG_TAG,
                "Initiating offloading process... Offloading the last stage of one of "+validatedPipelines.size()+"pipelines");

        //PHASE 1 - Preparation
        List<Long>  executionTimesByStage = new ArrayList<>(),
                    generatedDataByStage = new ArrayList<>();

        ////Fetch the costs for each stage
        ProfilingStageWrapper auxStage;
        for(PipelineConfiguration p : validatedPipelines){
            auxStage = (ProfilingStageWrapper) p.getLastStage();
            executionTimesByStage.add(auxStage.getAverageRunningTime());
            generatedDataByStage.add(auxStage.getAverageGeneratedDataSize());
        }

        ////Aux variables:
        long totalExecutionTime = getTotalExecutionTime();
        long optimalGeneratedData = getOptimalGeneratedData(generatedDataByStage);
        long optimalExecutionTime = getOptimalExecutionTime(executionTimesByStage);

        StageCostComputer decider =
                new MultiCriteriaStageCostComputer(totalExecutionTime,
                                                    optimalGeneratedData, optimalExecutionTime);

        //PHASE 2 - Decision
        int i, worst;
        ProfilingStageWrapper auxLastStage,
                mostExpensiveStage = (ProfilingStageWrapper) validatedPipelines.get(0).getLastStage();

        for(i=0, worst=0; i < validatedPipelines.size(); i++){

            auxLastStage = (ProfilingStageWrapper) validatedPipelines.get(i).getLastStage();

            if(!decider.isMoreExpensive(mostExpensiveStage, auxLastStage)){
                mostExpensiveStage = auxLastStage;
                worst = i;
            }
        }

        Log.d(LOG_TAG, "The most expensive stage is :"+mostExpensiveStage.getStageClass().getSimpleName());

        //PHASE 3 - Offloading

    }


    /*
     ************************************************************************
     * Multi-Criteria Decision Theory                                       *
     ************************************************************************
     */
    private long getTotalExecutionTime(){
        long total = 0;

        for(PipelineConfiguration p : validatedPipelines){
            for(Stage stage : p.getStages())
                total += ((ProfilingStageWrapper)stage).getAverageRunningTime();
        }

        return total;
    }

    private long getTotalGeneratedData(){
        long total = 0;

        for(PipelineConfiguration p : validatedPipelines)
           total +=  ((ProfilingStageWrapper)p.getLastStage()).getAverageGeneratedDataSize();

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

        public final static float TIME_WEIGHT = (float).7;
        public final static float DATA_WEIGHT = (float).3;

        public abstract float computeCost(ProfilingStageWrapper stage);

        public boolean isMoreExpensive(ProfilingStageWrapper baseStage, ProfilingStageWrapper stage){

            float cost1, cost2;

            cost1 = computeCost(baseStage);
            cost2 = computeCost(stage);

            return cost1 >= cost2;
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
        public float computeCost(ProfilingStageWrapper stage) {
            return  TIME_WEIGHT*timeUtilityFunction(stage.getAverageRunningTime()) +
                    DATA_WEIGHT*dataUtilityFunction(stage.getAverageGeneratedDataSize());

        }
    }
}
