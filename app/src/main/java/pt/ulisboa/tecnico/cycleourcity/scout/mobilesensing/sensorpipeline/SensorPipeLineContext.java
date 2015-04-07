package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.*;
import com.ideaimpl.patterns.pipeline.Error;

import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.data.AccelerometerSampleWindow;

/**
 * Created by rodrigo.jm.lourenco on 15/03/2015.
 */
public class SensorPipeLineContext implements PipelineContext {

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
