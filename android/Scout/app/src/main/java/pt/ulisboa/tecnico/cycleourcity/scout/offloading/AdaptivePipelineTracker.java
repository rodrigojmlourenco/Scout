package pt.ulisboa.tecnico.cycleourcity.scout.offloading;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Longs;
import com.ideaimpl.patterns.pipeline.Stage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.AdaptivePipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.NothingToRetrieveException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.exceptions.NothingToOffloadException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.OffloadingWrapperStage;

public class AdaptivePipelineTracker {

    //Tracked Pipeline
    private int currentConfig;
    private LinkedList<Stage> offloadedStages;
    private AdaptivePipeline pipeline;

    //Original Configuration Values
    private final int originalConfigCount;
    private final List<Stage> originalConfiguration;
    private float[] totalUtilityValues;

    //Configuration computation support values
    private final long optimalExecution, optimalTransmissionSize;
    private float timeWeight = 1, mobileCostWeight=0, transmissionWeight = 0;


    public AdaptivePipelineTracker(AdaptivePipeline pipeline){

        this.pipeline = pipeline;
        originalConfiguration = new ArrayList<>(pipeline.getAdaptiveStages());
        originalConfigCount = pipeline.getAdaptiveStages().size()+1;

        currentConfig = originalConfigCount;
        offloadedStages = new LinkedList<>();

        optimalExecution = 0;
        optimalTransmissionSize = computeOptimalTransmittedData(originalConfiguration);
    }


    /**
     * Computes the number of offloading operation that must be performed as to achieve
     * the ideal configuration, i.e. the one that maximizes the total utility function.
     * <br>
     * Depending on the computed value three scenarios arise:<br>
     *     <ul>
     *         <li>i = 0: Nothing is to be done, has the current configuration is the ideal one.</li>
     *         <li>i > 0: Offload as many stages as specified by the return.</li>
     *         <li>i < 0: Retrieve as many stages as specified by the return.</li>
     *     </ul>
     * @return The number off stages that must be offloaded or retrieved.
     */
    public int computeIdealOffloadIterations(){
        return currentConfig - computeIdealConfiguration();
    }

    /**
     * Given a set of weights, which define the priority given to either the time or transmitted
     * data metrics, this methods updates the total utility values for each possible configuration.

     * @param timeWeight Priority given to execution the time metric
     * @param mobileCostWeight Priority given to the mobile cost, a sub-set of the transmitted data metric.
     * @param transmissionWeight Priority given to the transmitted data.
     */
    public void updateWeights(float timeWeight, float mobileCostWeight, float transmissionWeight){

        this.timeWeight         = timeWeight;
        this.mobileCostWeight   = mobileCostWeight;
        this.transmissionWeight = transmissionWeight;

        totalUtilityValues = computeConfigurationsTotalUtilities();
    }


    /*
     ************************************************
     * Multi-Criteria Decision Theory Computations  *
     ************************************************
     */

    private int computeIdealConfiguration(){
        float bestUtility = Floats.max(totalUtilityValues);
        int ideal = 0;
        for(; ideal < originalConfigCount; ideal++)
            if (bestUtility == totalUtilityValues[ideal])
                return ideal+1;

        return 0;
    }

    private long computeOptimalTransmittedData(List<Stage> originalConfig){

        int possibilities = originalConfig.size()+1;
        long[] transmittedData = new long[possibilities];

        transmittedData[0] = ((OffloadingWrapperStage)originalConfig.get(0)).getAverageInputDataSize();
        for(int i=1, j=0; i < possibilities; i++, j++)
            transmittedData[i] = ((OffloadingWrapperStage)originalConfig.get(j)).getAverageGeneratedDataSize();

        long min = Longs.min(transmittedData);
        return min;
    }

    private float computeExecutionTimeUtility(long optimalExecutionTime, long realExecutionTime){

        if(realExecutionTime==0) return 0;

        return (float)(optimalExecutionTime - realExecutionTime)/realExecutionTime;
    }

    private float computeTransmissionUtility(long optimalTransmittedData, long realTransmittedDataSize){
        return (float)(optimalTransmittedData-realTransmittedDataSize)/realTransmittedDataSize;
    }

    private float computeTotalUtility(
            long optimalExecutionTime, long optimalTransmissionSize,
            long configExecutionTime, long configTransmissionSize){

        float timeUtility = computeExecutionTimeUtility(optimalExecutionTime, configExecutionTime),
                transmissionUtility = computeTransmissionUtility(optimalTransmissionSize, configTransmissionSize);

        return timeWeight*timeUtility+(mobileCostWeight+transmissionWeight)*transmissionUtility;
    }


    private float[] computeConfigurationsTotalUtilities(){

        float[] totalUtilities = new float[originalConfigCount];

        //Special Case - Nothing is executed and all data is sent
        long sequentialExecutionTime = 0;
        long transmittedData = ((OffloadingWrapperStage)originalConfiguration.get(0)).getAverageInputDataSize();
        totalUtilities[0] = computeTotalUtility(optimalExecution, optimalTransmissionSize, sequentialExecutionTime, transmittedData);

        //Remaining cases
        for(int i=1; i < originalConfigCount; i++){
            OffloadingWrapperStage s = (OffloadingWrapperStage) originalConfiguration.get(i - 1);
            sequentialExecutionTime += s.getAverageRunningTime();
            transmittedData = s.getAverageGeneratedDataSize();
            totalUtilities[i] = computeTotalUtility(optimalExecution, optimalTransmissionSize, sequentialExecutionTime, transmittedData);
        }

        return totalUtilities;
    }


    /*
     ********************************************
     * Pipeline Manipulation                    *
     ********************************************
     */

    public AdaptivePipeline getPipeline(){return pipeline; }

    /**
     * Offloads the pipeline's tail stage
     * @return The offloaded stage wrapped by a OffloadingStageWrapper
     * @throws NothingToOffloadException if there are no more offloadable stages.
     */
    public OffloadingWrapperStage offloadStage()
            throws NothingToOffloadException {

        if(pipeline.getAdaptiveStages().isEmpty())
            throw new NothingToOffloadException();

        Stage stage = pipeline.removeStage();
        offloadedStages.add(stage);
        currentConfig--;
        return (OffloadingWrapperStage) stage;
    }

    /**
     * Retrieves, to the pipeline, the last offloaded stage.
     * @throws NothingToRetrieveException if there are no offloaded stages.
     */
    public void retrieveStage()
        throws NothingToRetrieveException {

        if(offloadedStages.isEmpty())
            throw new NothingToRetrieveException();

        pipeline.addStage(offloadedStages.removeLast());
        currentConfig++;
    }


}