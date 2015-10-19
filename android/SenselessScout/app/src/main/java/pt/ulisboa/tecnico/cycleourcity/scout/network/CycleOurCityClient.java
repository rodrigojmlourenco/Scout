package pt.ulisboa.tecnico.cycleourcity.scout.network;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

import pt.ulisboa.tecnico.cycleourcity.scout.network.stages.strategies.BatchingStrategy;
import pt.ulisboa.tecnico.cycleourcity.scout.network.stages.strategies.EagerStrategy;
import pt.ulisboa.tecnico.cycleourcity.scout.network.stages.strategies.UploadingStrategy;

/**
 * Created by rodrigo.jm.lourenco on 16/09/2015.
 */
public class CycleOurCityClient {

    private static CycleOurCityClient CLIENT = null;

    private UploadingStrategy strategy;

    private CycleOurCityClient(){
        strategy = new BatchingStrategy(100000); //100KB buffer as in Balasubramanian
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

    public synchronized HttpResponse upload(){
        return strategy.uploadRemaining();
    }
}
