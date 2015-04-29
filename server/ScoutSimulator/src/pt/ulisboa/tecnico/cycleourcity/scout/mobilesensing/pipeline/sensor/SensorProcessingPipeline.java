package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor;

import com.google.gson.JsonObject;

import java.util.Collection;

/**
 * Created by rodrigo.jm.lourenco on 26/03/2015.
 */
public interface SensorProcessingPipeline extends Runnable {

    public void pushSample(JsonObject sensorSample);

    public void pushSampleCollection(Collection<JsonObject> sampleCollection);

}
