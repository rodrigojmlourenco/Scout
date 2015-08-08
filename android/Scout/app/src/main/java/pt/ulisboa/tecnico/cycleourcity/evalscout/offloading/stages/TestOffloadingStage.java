package pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.stages;

import android.util.Log;

import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.profiling.pipelines.StageProfiler;

public class TestOffloadingStage extends OffloadingWrapperStage {

    protected final String LOG_TAG = "TestStage";
    private final int wait;
    private final int inputDataBytes, outputDataBytes;

    public TestOffloadingStage(String identifier, Stage stage, int waitMillis, int inputDataBytes, int outputDataBytes) {
        super(identifier, stage);
        this.wait = waitMillis;
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
    public long getAverageRunningTime() {
        return wait;
    }

    @Override
    public void execute(PipelineContext pipelineContext) {

        long startTime = 0, endTime;

        if(profilingEnabled) {
            startTime = System.nanoTime();
        }

        this.stage.execute(pipelineContext);

        if(profilingEnabled) {
            //Time Profiling
            endTime = System.nanoTime();
            executionTimes.add(endTime - startTime);

            dataSizes.add(new DataProfileInfo(inputDataBytes, outputDataBytes));
        }

        if(profilingEnabled)
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

            }else {
                if(StageProfiler.VERBOSE)
                    Log.d(StageProfiler.LOG_TAG, "["+identifier+"]:"
                            +(StageProfiler.NUM_PROFILING_SAMPLES-executionTimes.size())+" iterations to go.");
            }
    }
}