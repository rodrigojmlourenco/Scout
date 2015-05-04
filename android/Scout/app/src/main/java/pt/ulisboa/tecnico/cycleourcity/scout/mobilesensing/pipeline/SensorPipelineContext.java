package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.*;
import com.ideaimpl.patterns.pipeline.Error;

import java.util.List;


/**
 * Created by rodrigo.jm.lourenco on 15/03/2015.
 */
public class SensorPipelineContext implements PipelineContext {

    private JsonObject[] input;
    private JsonObject[] output;

    public JsonObject[] getInput(){
        return input;
    }

    public void setInput(JsonObject[] samples){
        input = samples;
    }

    public void clearInput(){ input = null; }

    public JsonObject[] getOutput(){ return output; }

    public void setOutput(JsonObject[] output){ this.output = output; }

    @Override
    public List<Error> getErrors() {
        return null;
    }
}