package pt.ulisboa.tecnico.cycleourcity.evalscout.network.stages.strategies;

import com.google.gson.JsonObject;

import org.apache.http.HttpResponse;

/**
 * Created by rodrigo.jm.lourenco on 18/09/2015.
 */
public interface UploadingStrategy {

    final String herokuURI = "https://quiet-beach-7008.herokuapp.com/myresource/post";

    public HttpResponse upload(JsonObject inference);
}
