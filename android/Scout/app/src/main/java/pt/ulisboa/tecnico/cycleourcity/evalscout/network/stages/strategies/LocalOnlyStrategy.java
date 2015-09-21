package pt.ulisboa.tecnico.cycleourcity.evalscout.network.stages.strategies;

import com.google.gson.JsonObject;

import org.apache.http.HttpResponse;

/**
 * Created by rodrigo.jm.lourenco on 18/09/2015.
 */
public class LocalOnlyStrategy implements UploadingStrategy{

    @Override
    public HttpResponse upload(JsonObject inference) {
        //Do nothing
        return null;
    }
}
