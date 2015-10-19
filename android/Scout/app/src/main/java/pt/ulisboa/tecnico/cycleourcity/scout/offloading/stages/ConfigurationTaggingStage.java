package pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.OffloadTracker;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.UnvalidatedPipelineException;

/**
 * The AdaptiveOffloadingTaggingStage is a custom stage, designed to tag each sample with the
 * corresponding missing stages, i.e., the stages that were offloaded.
 * <br>
 * By tagging each sensor sample with his missing stages, the server becomes able to
 * adjust itself in order to appropriately process those samples.
 */
public class ConfigurationTaggingStage implements Stage {

    private int pUID;
    private OffloadTracker offloadTracker;

    public void setPipelineUID(int pUID){
        this.pUID = pUID;
    }

    public void setOffloadTracker(OffloadTracker tracker){
        this.offloadTracker = tracker;
        tracker.registerPipeline(pUID);
    }

    @Override
    public void execute(PipelineContext pipelineContext) {

        SensorPipelineContext ctx = (SensorPipelineContext)pipelineContext;
        JsonObject[] input = ctx.getInput();
        List errors = ctx.getErrors();

        if(input==null || (errors != null && errors.size() > 0)) return;

        if(input.length > 1){
            JsonObject result = new JsonObject();
            JsonArray batch = new JsonArray();
            JsonArray config = new JsonArray();

            try {
                Gson gson = new Gson();
                config = offloadTracker.getConfiguration(this.pUID);
                Log.d("CONFIG", gson.toJson(config));
            } catch (UnvalidatedPipelineException e) {
                e.printStackTrace();
            }

            for(JsonObject sample : input){
                batch.add(sample);
            }

            result.add("frames", batch);
            result.add(OffloadTracker.CONFIGURATION, config);

            JsonObject[] output = new JsonObject[1];
            output[0] = result;
            ctx.setInput(output);

        }else {
            for (JsonObject sample : input) {
                try {
                    sample.add(OffloadTracker.CONFIGURATION, offloadTracker.getConfiguration(this.pUID));
                } catch (UnvalidatedPipelineException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        //Testing TODO: remvoer
        try {
            JsonArray config = offloadTracker.getConfiguration(pUID);
            if(config != null)
                Log.d("CONFIG", pUID+" : "+String.valueOf(config));
        } catch (UnvalidatedPipelineException e) {
            e.printStackTrace();
        }
    }
}