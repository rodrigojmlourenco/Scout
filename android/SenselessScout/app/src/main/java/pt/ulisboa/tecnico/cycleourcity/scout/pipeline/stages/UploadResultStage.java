package pt.ulisboa.tecnico.cycleourcity.scout.pipeline.stages;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.network.CycleOurCityClient;

public class UploadResultStage implements Stage {

    private CycleOurCityClient client;

    public UploadResultStage(){
        client = CycleOurCityClient.getInstance();
    }

    @Override
    public void execute(PipelineContext pipelineContext) {

        SensorPipelineContext ctx = (SensorPipelineContext)pipelineContext;
        JsonObject[] output = ctx.getInput();

        if((ctx.getErrors()!=null && !ctx.getErrors().isEmpty()) || output==null || output.length <= 0)
            return;

        //for (final JsonObject result : output)
        //    client.upload(result);

    }
}
