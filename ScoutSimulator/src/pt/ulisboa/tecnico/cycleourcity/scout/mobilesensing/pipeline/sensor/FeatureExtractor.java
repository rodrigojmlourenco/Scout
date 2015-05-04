package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor;

import com.google.gson.JsonObject;

/**
 * Created by rodrigo.jm.lourenco on 24/04/2015.
 */
public interface FeatureExtractor {

    public JsonObject[] consumeExtractedFeatures();
}
