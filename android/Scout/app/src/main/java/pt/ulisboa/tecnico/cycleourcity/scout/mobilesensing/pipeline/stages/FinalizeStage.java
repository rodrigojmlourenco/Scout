package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.stages;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;

/**
 * Created by rodrigo.jm.lourenco on 30/04/2015.
 */
public class FinalizeStage implements Stage {
    @Override
    public void execute(PipelineContext pipelineContext) {
        JsonObject[] input = ((SensorPipelineContext)pipelineContext).getInput();
        ((SensorPipelineContext)pipelineContext).setOutput(input);
    }
}
