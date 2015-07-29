package pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages;

import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

public class TestOffloadingStage extends OffloadingWrapperStage {

    protected final String LOG_TAG = "TestStage";
    private final int wait;
    private final int inputDataBytes, outputDataBytes;

    public TestOffloadingStage(Stage stage, int waitMillis, int inputDataBytes, int outputDataBytes, String identifier) {
        super(identifier, stage); //TODO corrigir o identificador
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
        for(int i=0; i < this.wait; i++ );
    }
}
