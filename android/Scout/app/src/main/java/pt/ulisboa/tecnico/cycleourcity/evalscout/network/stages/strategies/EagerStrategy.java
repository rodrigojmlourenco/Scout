package pt.ulisboa.tecnico.cycleourcity.evalscout.network.stages.strategies;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

/**
 * Created by rodrigo.jm.lourenco on 18/09/2015.
 */
public class EagerStrategy implements UploadingStrategy{


    @Override
    public HttpResponse upload(JsonObject inference) {
        Gson gson = new Gson();

        HttpPost httpost = new HttpPost(herokuURI);
        DefaultHttpClient httpClient = new DefaultHttpClient();

        Log.d("NET", gson.toJson(inference));

        try {

            StringEntity entity = new StringEntity(gson.toJson(inference));
            httpost.setEntity(entity);
            httpost.setHeader("Accept", "application/json");
            httpost.setHeader("Content-type", "application/json");

            // Execute HTTP Post Request
            return httpClient.execute(httpost);


        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
