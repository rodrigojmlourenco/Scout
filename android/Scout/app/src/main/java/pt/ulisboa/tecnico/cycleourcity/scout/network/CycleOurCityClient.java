package pt.ulisboa.tecnico.cycleourcity.scout.network;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import objectexplorer.ObjectGraphMeasurer;

/**
 * Created by rodrigo.jm.lourenco on 16/09/2015.
 */
public class CycleOurCityClient {

    private static CycleOurCityClient CLIENT = null;

    private final String herokuURI = "https://quiet-beach-7008.herokuapp.com/myresource/post";

    private final DefaultHttpClient herokuClient;
    private final HttpPost herokuStore;

    private CycleOurCityClient(){
        herokuClient = new DefaultHttpClient();
        herokuStore = new HttpPost(herokuURI);
    }

    static public CycleOurCityClient getInstance(){
        synchronized (CycleOurCityClient.class){
            if(CLIENT == null)
                CLIENT = new CycleOurCityClient();
        }

        return  CLIENT;
    }

    public synchronized HttpResponse upload(JsonObject pipelineResult){

        Gson gson = new Gson();

        HttpPost httpost = new HttpPost(herokuURI);
        //DefaultHttpClient httpClient = new DefaultHttpClient();

        Log.d("NET", gson.toJson(pipelineResult));

        try {

            StringEntity entity = new StringEntity(gson.toJson(pipelineResult));
            httpost.setEntity(entity);
            httpost.setHeader("Accept", "application/json");
            httpost.setHeader("Content-type", "application/json");

            // Execute HTTP Post Request
            return herokuClient.execute(httpost);


        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }

        return null;
    }
}
