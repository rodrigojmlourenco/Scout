package pt.ulisboa.tecnico.cycleourcity.evalscout.network;

import com.google.gson.JsonObject;

import org.apache.http.HttpResponse;

import pt.ulisboa.tecnico.cycleourcity.evalscout.network.stages.strategies.BatchingStrategy;
import pt.ulisboa.tecnico.cycleourcity.evalscout.network.stages.strategies.UploadingStrategy;

public class CycleOurCityClient {

    private static CycleOurCityClient CLIENT = null;

    private UploadingStrategy strategy;

    private CycleOurCityClient(){
        strategy = new BatchingStrategy(100000); //100KB buffer
    }

    static public CycleOurCityClient getInstance(){
        synchronized (CycleOurCityClient.class){
            if(CLIENT == null)
                CLIENT = new CycleOurCityClient();
        }

        return  CLIENT;
    }

    public synchronized HttpResponse upload(JsonObject pipelineResult){
        return strategy.upload(pipelineResult);
    }
}
