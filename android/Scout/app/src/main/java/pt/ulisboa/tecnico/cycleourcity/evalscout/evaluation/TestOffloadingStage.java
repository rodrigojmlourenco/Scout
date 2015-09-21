package pt.ulisboa.tecnico.cycleourcity.evalscout.evaluation;

import android.util.Log;

import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.profiling.pipelines.StageProfiler;
import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.stages.OffloadingWrapperStage;

public class TestOffloadingStage extends OffloadingWrapperStage {

    protected final String LOG_TAG = "TestStage";
    private int wait;
    private final int inputDataBytes, outputDataBytes;


    public TestOffloadingStage(String identifier, Stage stage, int waitMillis, int inputDataBytes, int outputDataBytes) {
        super(identifier, stage);
        this.inputDataBytes = inputDataBytes;
        this.outputDataBytes= outputDataBytes;
    }

    @Override
    public long getAverageInputDataSize() {
        return inputDataBytes;
    }

    @Override
    public long getAverageGeneratedDataSize() {
        return outputDataBytes;
    }

    @Override
    public void execute(PipelineContext pipelineContext) {

        long startTime, endTime;

        startTime = System.nanoTime();

        this.stage.execute(pipelineContext);

        //Time Profiling
        endTime = System.nanoTime();
        executionTimes.add(endTime - startTime);

        dataSizes.add(new DataProfileInfo(inputDataBytes, outputDataBytes));

        if (!profiler.hasBeenModeled(identifier) && isInitialMonitoringComplete()) {

            if(StageProfiler.VERBOSE)
                Log.d(StageProfiler.LOG_TAG, "Generating a model for the stage " + identifier + ".");

            profiler.generateStageModel(
                    identifier,
                    stage.getClass().getCanonicalName(),
                    getAverageRunningTime(),
                    getAverageInputDataSize(),
                    getAverageGeneratedDataSize());

            this.profilingEnabled = false;

        }
        /*else {
            if(StageProfiler.VERBOSE)
                Log.d(StageProfiler.LOG_TAG, "["+identifier+"]:"
                        +(StageProfiler.NUM_PROFILING_SAMPLES-executionTimes.size())+" iterations to go.");
        }*/
    }
}