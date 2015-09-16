package pt.ulisboa.tecnico.cycleourcity.scout.network;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.UnsupportedEncodingException;

import objectexplorer.ObjectGraphMeasurer;

/**
 * Created by rodrigo.jm.lourenco on 16/09/2015.
 */
public class CycleOurCityClient {

    private static CycleOurCityClient CLIENT = null;

    private CycleOurCityClient(){}

    static public CycleOurCityClient getInstance(){
        synchronized (CycleOurCityClient.class){
            if(CLIENT == null)
                CLIENT = new CycleOurCityClient();
        }

        return  CLIENT;
    }

    public void upload(JsonObject pipelineResult){

        Gson gson = new Gson();

        Log.d("COC", "ObjectGraph" + ObjectGraphMeasurer.measure(pipelineResult));

        try {
            Log.d("COC", "Classic"+ gson.toJson(pipelineResult).getBytes("UTF-8").length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }
}
