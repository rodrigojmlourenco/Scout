package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor;

import com.google.gson.JsonObject;

import java.util.Collection;

/**
 * Created by rodrigo.jm.lourenco on 24/04/2015.
 */
public interface FeatureExtractor {

    public JsonObject[] consumeExtractedFeatures();
}
