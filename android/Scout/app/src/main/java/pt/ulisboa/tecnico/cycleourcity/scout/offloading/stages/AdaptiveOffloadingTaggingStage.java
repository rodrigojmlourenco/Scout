package pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.util.UUID;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.ConfigurationCaretaker;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.AdaptiveOffloadingManager;

/**
 * The AdaptiveOffloadingTaggingStage is a custom stage, designed to tag each sample with the
 * corresponding missing stages, i.e., the stages that were offloaded.
 * <br>
 * By tagging each sensor sample with his missing stages, the server becomes able to
 * adjust itself in order to apropriately process those samples.
 */
public class AdaptiveOffloadingTaggingStage implements Stage {


    private Object stateLock = new Object();
    private boolean wasModified = false;
    private ConfigurationCaretaker caretaker;

    public AdaptiveOffloadingTaggingStage(ConfigurationCaretaker caretaker){
        this.caretaker = caretaker;
    }

    public void setWasModified(){
        synchronized (stateLock) {
            wasModified = true;
        }
    }

    @Override
    public void execute(PipelineContext pipelineContext) {

        synchronized (stateLock) {
            if (!wasModified) return;
        }


        SensorPipelineContext ctx = (SensorPipelineContext)pipelineContext;
        JsonObject[] input = ctx.getInput();

        JsonParser parser = new JsonParser();
        for(JsonObject sample : input){

            JsonArray missingStages = new JsonArray();
            for (Stage missing : caretaker.getMissingStages()){
                JsonElement missingStageName = parser.parse(((ProfilingStageWrapper) missing).getStageClass().getName());
                missingStages.add(missingStageName);
            }

            //TODO: corrigir "_config" nao devia estar hardcoded
            sample.add("_config", missingStages);
        }
    }
}