package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor;

import java.util.LinkedList;
import java.util.Queue;

import edu.mit.media.funf.json.IJsonObject;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.data.LocationSample;

/**
 * Created by rodrigo.jm.lourenco on 26/03/2015.
 */
public class LocationPipeline implements ISensorPipeline {

    private final static String LOG_TAG = "LocationPipeline";

    private Queue<LocationSample> sampleQueue;

    public LocationPipeline(){
        this.sampleQueue = new LinkedList<>();
    }

    @Override
    public void pushSample(IJsonObject sensorSample) {
        this.sampleQueue.add(new LocationSample(sensorSample));
    }
}
