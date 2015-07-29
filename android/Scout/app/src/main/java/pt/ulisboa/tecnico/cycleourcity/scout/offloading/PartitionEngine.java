package pt.ulisboa.tecnico.cycleourcity.scout.offloading;

import android.util.Log;

import com.google.common.primitives.Longs;
import com.ideaimpl.patterns.pipeline.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.AdaptivePipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.AdaptiveOffloadingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.InvalidOffloadingStageException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.OverearlyOffloadException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.TaggingStageMissingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.exceptions.NoAdaptivePipelineValidatedException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.exceptions.NothingToOffloadException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.ConfigurationTaggingStage;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.OffloadingWrapperStage;


public class PartitionEngine {

    public final static float DEFAULT_ENERGY_WEIGHT = 1,
                                DEFAULT_DATA_WEIGHT = 0;

    private final boolean VERBOSE = true;

    private final String LOG_TAG = AdaptiveOffloadingManager.LOG_TAG;
    private final String NAME_TAG = getClass().getSimpleName();

    private ArrayList<AdaptivePipeline> validatedPipelines;

    private final OffloadTracker offloadTracker;

    private final PartitionEngineState internalState;

    private float energyWeight, dataWeight;

    protected PartitionEngine(OffloadTracker tracker){
        validatedPipelines = new ArrayList<>();
        offloadTracker = tracker;

        internalState = new PartitionEngineState();

        energyWeight= DEFAULT_ENERGY_WEIGHT;
        dataWeight  = DEFAULT_DATA_WEIGHT;
    }

    protected void setEnergyWeight(float weight){
        energyWeight = weight;
    }

    protected float getEnergyWeight(){
        return energyWeight;
    }

    protected void setDataWeight(float weight){
        dataWeight = weight;
    }

    protected float getDataWeight(){
        return dataWeight;
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

        this.validatedPipelines.add(pipeline);
    }

    @Deprecated
    public void offloadMostExpensiveStage()
            throws NothingToOffloadException, OverearlyOffloadException, NoAdaptivePipelineValidatedException {

        try {
            testOffloadMostExpensiveStage();
        }catch (ArithmeticException e){
            throw new OverearlyOffloadException();
        }
    }

    public void testOffloadMostExpensiveStage() throws NothingToOffloadException {

        if(validatedPipelines.isEmpty()) throw new NothingToOffloadException();

        if(VERBOSE)
            OffloadingLogger.log(this.getClass().getSimpleName(),
                    "[1] Initiating offloading process... Offloading the last stage of one of " + validatedPipelines.size() + "pipelines");

        //1. Update internal state:
        //  - Current configuration metrics values
        //  - Optimal configuration metric values
        this.internalState.updateInternalState(validatedPipelines);


        //2. Identify the worst stage
        StageCostComputer decider =
                new RevisedMultiCriteriaCostComputer(
                        energyWeight, dataWeight,
                        internalState.getOriginalExecutionTime(),
                        internalState.getOriginalTransmittedDataSize(),
                        internalState.getOptimalExecutionTime(),
                        internalState.getOptimalTransmittedDataSize());

        AdaptivePipeline offloadCandidate = decider.getOptimalOffloadingPipeline(validatedPipelines);

        List<OffloadingWrapperStage> offloadingStageOptions;
        if(VERBOSE){
            offloadingStageOptions = new ArrayList<>();
            for(AdaptivePipeline p : validatedPipelines)
                offloadingStageOptions.add((OffloadingWrapperStage) p.getLastAdaptiveStage());
        }


        //3 - Offload the worst stage
        OffloadingWrapperStage offloadedStage= (OffloadingWrapperStage) offloadCandidate.removeStage();
        offloadTracker.markOffloadedStage(offloadCandidate, offloadedStage);

        if (VERBOSE)
            OffloadingLogger.log(NAME_TAG, dumpInfo(offloadedStage, offloadingStageOptions));


        //4 - Remove the pipeline if it become unoffloadable
        if(offloadCandidate.getAdaptiveStages().isEmpty())
            validatedPipelines.remove(offloadCandidate);
    }

    protected void clearState(){
        this.validatedPipelines.clear();
    }


    /*
     ************************************************************************
     * Multi-Criteria Decision Theory                                       *
     ************************************************************************
     */
    public static abstract class StageCostComputer {

        protected final float energyWeight, dataWeight;

        public StageCostComputer(float energyWeight, float dataWeight){
            this.energyWeight = energyWeight;
            this.dataWeight = dataWeight;
        }

        public abstract float computeCost(OffloadingWrapperStage stage);

        public boolean isMoreExpensive(OffloadingWrapperStage baseStage, OffloadingWrapperStage stage){

            float cost1, cost2;

            cost1 = computeCost(baseStage);
            cost2 = computeCost(stage);

            return cost1 <= cost2;
        }

        public final AdaptivePipeline getOptimalOffloadingPipeline(List<AdaptivePipeline> pipelines){

            int i, worst;

            OffloadingWrapperStage
                    auxLastStage,
                    mostExpensiveStage = (OffloadingWrapperStage) pipelines.get(0).getLastAdaptiveStage();

            for(i=0, worst=0; i < pipelines.size(); i++){

                auxLastStage = (OffloadingWrapperStage) pipelines.get(i).getLastAdaptiveStage();

                if(!isMoreExpensive(mostExpensiveStage, auxLastStage)) {
                    mostExpensiveStage = auxLastStage;
                    worst = i;
                }
            }

            return pipelines.get(worst);
        }
    }



    public static class RevisedMultiCriteriaCostComputer extends StageCostComputer{

        private final long originalExecutionTime, originalDataSize,
                            optimalExecutionTime, optimalDataSize;

        public RevisedMultiCriteriaCostComputer(float energyWeight, float dataWeight,
                                                long originalExecutionTime, long originalDataSize,
                                                long optimalExecutionTime, long optimalDataSize){

            super(energyWeight, dataWeight);
            this.originalExecutionTime = originalExecutionTime;
            this.optimalExecutionTime = optimalExecutionTime;
            this.originalDataSize = originalDataSize;
            this.optimalDataSize = optimalDataSize;
        }

        private float timeUtilityFunction(OffloadingWrapperStage stage){
            float newTime = originalExecutionTime - stage.getAverageRunningTime();
            return (optimalExecutionTime - newTime)/newTime;
        }

        private float dataUtilityFunction(OffloadingWrapperStage stage){
            float newDataSize = originalDataSize - stage.getAverageGeneratedDataSize() + stage.getAverageInputDataSize();
            return (optimalDataSize-newDataSize)/newDataSize;
        }

        @Override
        public float computeCost(OffloadingWrapperStage stage) {
            return  energyWeight*timeUtilityFunction(stage) +
                    dataWeight*dataUtilityFunction(stage);
        }
    }



    /*
     ****************************************************************************
     * Information Logging                                                      *
     ****************************************************************************
     */

    private String dumpInfo(OffloadingWrapperStage choice, List<OffloadingWrapperStage> stages){
        return "{ name: \""+NAME_TAG+"\", "+
                "chosen: "+choice.dumpInfo()+", "+
                "options: "+dumpOffloadingOptionsInfo(stages)+"}";
    }

    private String dumpOffloadingOptionsInfo(List<OffloadingWrapperStage> stages){
        String info = "[ ";

        int i=1, options = stages.size();
        for(OffloadingWrapperStage s : stages)
            info += s.dumpInfo() + (i++ < options ? ", " : "]");

        return info;
    }


    /*
     ************************************************************************
     * PartitionEngineState for support                                     *
     ************************************************************************
     */
    private class PartitionEngineState {

        private long originalExecutionTime, originalTransmittedDataSize,
                optimalExecutionTime, optimalTransmittedDataSize;


        private long computeOriginalExecutionTime(List<AdaptivePipeline> pipelines){

            long sequentialExecutionTime = 0;

            for(AdaptivePipeline p : pipelines)
                for (Stage s : p.getAdaptiveStages())
                    sequentialExecutionTime += ((OffloadingWrapperStage) s).getAverageRunningTime();

            return sequentialExecutionTime;

        }

        private long computeOriginalTransmittedDataSize(List<AdaptivePipeline> pipelines){
            long totalTransmittedDataSize = 0;

            for(AdaptivePipeline p : pipelines)
                totalTransmittedDataSize +=
                        ((OffloadingWrapperStage)p.getLastAdaptiveStage()).getAverageGeneratedDataSize();

            return totalTransmittedDataSize;
        }

        private long computeNewSequentialExecutionTime(OffloadingWrapperStage lastStage){
            return originalExecutionTime - lastStage.getAverageRunningTime();
        }

        private long computeNewTransmittedDataSize(OffloadingWrapperStage lastStage){
            return originalTransmittedDataSize - lastStage.getAverageGeneratedDataSize() + lastStage.getAverageInputDataSize();
        }

        public void updateInternalState(List<AdaptivePipeline> activePipelines){

            int size = activePipelines.size();

            //1. Compute current configuration metrics
            originalExecutionTime = computeOriginalExecutionTime(activePipelines);
            originalTransmittedDataSize = computeOriginalTransmittedDataSize(activePipelines);

            //2. Compute possible metrics for each configuration
            long[] possibleExecutionTimes = new long[size],
                    possibleTransmittedDataSizes = new long[size];

            OffloadingWrapperStage auxLastStage;
            for(int i=0; i < size ; i++){
                auxLastStage = (OffloadingWrapperStage)activePipelines.get(i).getLastAdaptiveStage();
                possibleExecutionTimes[i] = computeNewSequentialExecutionTime(auxLastStage);
                possibleTransmittedDataSizes[i] = computeNewTransmittedDataSize(auxLastStage);
            }

            //3. Compute the optimal configuration for each metric
            optimalExecutionTime = Longs.min(possibleExecutionTimes);
            optimalTransmittedDataSize = Longs.min(possibleTransmittedDataSizes);
        }

        public long getOriginalExecutionTime(){return originalExecutionTime; }
        public long getOriginalTransmittedDataSize() { return  originalTransmittedDataSize; }
        public long getOptimalExecutionTime() { return optimalExecutionTime; }
        public long getOptimalTransmittedDataSize() { return optimalTransmittedDataSize; }
    }

    /*
     ************************************************************
     * Deprecated Functions - TODO remove                       *
     ************************************************************
     */
    /**
     * @see <a href="http://www.utdallas.edu/~cxl137330/courses/spring14/AdvRTS/protected/slides/43.pdf">SociableSense</a>
     */
    @Deprecated
    public static class MultiCriteriaStageCostComputer extends StageCostComputer{

        private final long
                totalExecutionTime,
                optimalGeneratedData,
                optimalExecutionTime;


        protected MultiCriteriaStageCostComputer(float energyWeight, float dataWeight,
                                                 long totalExecutionTime,
                                                 long optimalGeneratedData, long optimalExecutionTime){

            super(energyWeight, dataWeight);

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
        public float computeCost(OffloadingWrapperStage stage) {
            return  energyWeight*timeUtilityFunction(stage.getAverageRunningTime()) +
                    dataWeight*dataUtilityFunction(stage.getAverageGeneratedDataSize());

        }
    }

    private long getOptimalGeneratedData(List<Long> dataSizes){

        long totalGeneratedData = getCurrentGeneratedData();
        List<Long> newSizes = new ArrayList<>();

        for(Long size : dataSizes)
            newSizes.add(totalGeneratedData - size);

        return Collections.min(newSizes);
    }

    private long getOptimalExecutionTime(List<Long> executionTimes){

        long totalTime = getCurrentSequentialExecutionTime();
        List<Long> newTimes = new ArrayList<>();

        for(Long time : executionTimes)
            newTimes.add(totalTime-time);

        return Collections.min(newTimes);
    }

    @Deprecated
    public Stage oldOffloadMostExpensiveStage()
            throws NoAdaptivePipelineValidatedException, NothingToOffloadException, OverearlyOffloadException {

        if(validatedPipelines.isEmpty()) throw new NothingToOffloadException();

        if(VERBOSE)
            OffloadingLogger.log(this.getClass().getSimpleName(),
                    "[1] Initiating offloading process... Offloading the last stage of one of " + validatedPipelines.size() + "pipelines");

        //PHASE 1 - Preparation
        long originalTotalSentData, originalSequentialExecutionTime;


        List<Long>  executionTimesByStage = new ArrayList<>(),
                generatedDataByStage = new ArrayList<>();

        List<OffloadingWrapperStage> offloadingStageOptions = new ArrayList<>();

        ////Fetch the costs for each stage
        OffloadingWrapperStage auxStage;
        for(AdaptivePipeline p : validatedPipelines){

            auxStage = (OffloadingWrapperStage) p.getLastAdaptiveStage();

            if(auxStage==null){
                if(VERBOSE) OffloadingLogger.log(this.getClass().getSimpleName(), p.getClass().getSimpleName() + " has no more stages");
                validatedPipelines.remove(p);
                continue;
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
            totalExecutionTime = getCurrentSequentialExecutionTime();
            totalGeneratedData = getCurrentGeneratedData();
            optimalGeneratedData = getOptimalGeneratedData(generatedDataByStage);
            optimalExecutionTime = getOptimalExecutionTime(executionTimesByStage);
        }catch (ArithmeticException e){
            throw new OverearlyOffloadException();
        }

        StageCostComputer decider =
                new MultiCriteriaStageCostComputer(
                        energyWeight, dataWeight,
                        totalExecutionTime,
                        optimalGeneratedData, optimalExecutionTime);

        //PHASE 2 - Decision
        int i, worst;
        OffloadingWrapperStage
                auxLastStage,
                mostExpensiveStage = (OffloadingWrapperStage) validatedPipelines.get(0).getLastAdaptiveStage();

        for(i=0, worst=0; i < validatedPipelines.size(); i++){

            auxLastStage = (OffloadingWrapperStage) validatedPipelines.get(i).getLastAdaptiveStage();

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

        offloadTracker.markOffloadedStage(p, (OffloadingWrapperStage) offloadedStage);

        //PHASE 4 - Adjust the pipelines
        if(p.getAdaptiveStages().isEmpty())
            validatedPipelines.remove(p);

        return offloadedStage;
    }

    private long getCurrentSequentialExecutionTime(){

        long total = 0;

        for(AdaptivePipeline p : validatedPipelines){
            for(Stage stage : p.getAdaptiveStages())
                total += ((OffloadingWrapperStage)stage).getAverageRunningTime();
        }

        return total;
    }

    private long getCurrentGeneratedData(){
        long total = 0;

        for(AdaptivePipeline p : validatedPipelines)
            total += ((OffloadingWrapperStage) p.getLastAdaptiveStage()).getAverageGeneratedDataSize();

        return total;
    }


    /*
     ************************************************************
     * NEW PARTITION ENGINE SCHEME                              *
     ************************************************************
     */
}


