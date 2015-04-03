package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor;

import edu.mit.media.funf.json.IJsonObject;

/**
 * Created by rodrigo.jm.lourenco on 26/03/2015.
 */
public interface ISensorPipeline  {

    public void pushSample(IJsonObject sensorSample);
}
