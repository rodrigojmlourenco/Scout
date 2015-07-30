package pt.ulisboa.tecnico.cycleourcity.scout.offloading;

import android.util.Log;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Longs;
import com.ideaimpl.patterns.pipeline.Stage;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.AdaptivePipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.exceptions.NothingToOffloadException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.OffloadingWrapperStage;

public class AdaptivePipelineTracker {

    private int currentConfig;
    private final int originalConfigCount;
    private final List<Stage> originalConfiguration;

    private float[] configsTotalUtility;

    private AdaptivePipeline pipeline;
    private List<Stage> missingStages;

    private float timeWeight = 1, mobileCostWeight=0, transmissionWeight = 0;

    public AdaptivePipelineTracker(AdaptivePipeline pipeline){

        originalConfigCount = pipeline.getAdaptiveStages().size()+1;
        currentConfig = originalConfigCount;
        originalConfiguration = pipeline.getAdaptiveStages();

        this.pipeline = pipeline;
        missingStages = new ArrayList<>();

    }

    public AdaptivePipelineTracker(AdaptivePipeline pipeline,
                                      float timeWeight, float mobileCostWeight, float transmissionWeight){

        originalConfigCount = pipeline.getAdaptiveStages().size()+1;
        currentConfig = originalConfigCount;
        originalConfiguration = pipeline.getAdaptiveStages();

        this.pipeline = pipeline;
        missingStages = new ArrayList<>();

        updateWeights(timeWeight, mobileCostWeight, transmissionWeight);
    }

    public void updateWeights(float timeWeight, float mobileCostWeight, float transmissionWeight){

        this.timeWeight         = timeWeight;
        this.mobileCostWeight   = mobileCostWeight;
        this.transmissionWeight = transmissionWeight;

        configsTotalUtility = computeConfigurationsTotalUtilities(pipeline);
    }

    public List<Stage> getCurrentConfiguration(){
        return pipeline.getAdaptiveStages();
    }

    public List<Stage> getMissingStages(){
        return missingStages;
    }

    public int computeIdealOffloadIterations(){
        return originalConfigCount - computeIdealConfiguration();
    }

    private int computeIdealConfiguration(){
        float bestUtility = Floats.max(configsTotalUtility);
        int ideal = 0;
        for(; ideal < originalConfigCount; ideal++)
            if (bestUtility == configsTotalUtility[ideal])
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


    private float[] computeConfigurationsTotalUtilities(AdaptivePipeline pipeline){

        float[] totalUtilities = new float[originalConfigCount];

        long optimalExecution = 0,
                optimalTransmissionSize = computeOptimalTransmittedData(originalConfiguration);
        //For each possible configuration compute total utility
        // then add that value to the array, in the position corresponding to that config

        //Special Case - Nothing is executed and all data is sent
        long executionTime;
        long transmittedData = ((OffloadingWrapperStage)pipeline.getAdaptiveStages().get(0)).getAverageInputDataSize();
        totalUtilities[0] = computeTotalUtility(optimalExecution, optimalTransmissionSize, 0, transmittedData);
        for(int i=1; i < originalConfigCount; i++){
            OffloadingWrapperStage s = (OffloadingWrapperStage) pipeline.getAdaptiveStages().get(i-1);
            executionTime = s.getAverageRunningTime();
            transmittedData = s.getAverageGeneratedDataSize();
            totalUtilities[i] = computeTotalUtility(optimalExecution, optimalTransmissionSize, executionTime, transmittedData);
        }

        //ArrayUtils.reverse(totalUtilities);

        return totalUtilities;
    }

    public AdaptivePipeline getPipeline(){return pipeline; }


    /*
     ********************************************
     * Pipeline Manipulation                    *
     ********************************************
     */
    public OffloadingWrapperStage offloadStage()
            throws NothingToOffloadException {

        if(pipeline.getAdaptiveStages().isEmpty())
            throw new NothingToOffloadException();

        Stage stage = pipeline.removeStage();
        missingStages.add(stage);
        return (OffloadingWrapperStage) stage;
    }

    public void addLoadStage(){
        throw new UnsupportedOperationException();
    }

}