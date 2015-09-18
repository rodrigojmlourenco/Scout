package pt.ulisboa.tecnico.cycleourcity.scout.network.stages.strategies;

import com.google.gson.JsonObject;

import org.apache.http.HttpResponse;

public class BatchingStrategy implements UploadingStrategy{

    

    @Override
    public HttpResponse upload(JsonObject inference) {
        return null;
    }
}
