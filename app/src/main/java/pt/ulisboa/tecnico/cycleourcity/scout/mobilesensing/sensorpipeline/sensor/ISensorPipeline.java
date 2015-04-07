package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor;

import com.google.gson.JsonObject;

import edu.mit.media.funf.json.IJsonObject;

/**
 * Created by rodrigo.jm.lourenco on 26/03/2015.
 */
public interface ISensorPipeline extends Runnable {

    public void pushSample(IJsonObject sensorSample);

    public void pushSample(JsonObject sensorSample);
}
