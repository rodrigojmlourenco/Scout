package pt.ulisboa.tecnico.cycleourcity.scout.network.stages.strategies;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * Created by rodrigo.jm.lourenco on 18/09/2015.
 */
public class EagerStrategy implements UploadingStrategy{

    private ExecutorService executor = Executors.newFixedThreadPool(5);

    @Override
    public HttpResponse upload(JsonObject inference) {
        executor.execute(new UploadTask(inference));
        return null;
    }

    @Override
    public HttpResponse uploadRemaining() {
        return null;
    }

    private class UploadTask implements Runnable{

        private final Gson gson = new Gson();
        private final JsonObject data;

        public UploadTask(JsonObject data){
            this.data = data;
        }

        @Override
        public void run() {

            try {

                HttpPost httpost = new HttpPost(herokuURI);
                DefaultHttpClient httpClient = new DefaultHttpClient();

                String udata = gson.toJson(data);

                StringEntity entity = new StringEntity(udata);
                httpost.setEntity(entity);
                httpost.setHeader("Accept", "application/json");
                httpost.setHeader("Content-type", "application/json");

                Log.e("EagerStrategy", "Attempting upload");


                HttpResponse response = httpClient.execute(httpost);
                HttpEntity httpEntity = response.getEntity();
                String result = EntityUtils.toString(httpEntity);

                Log.e("EagerStrategy", response.getStatusLine() + ": "+  result + " " +udata.getBytes("UTF-8").length);


            } catch (IOException e) {
                e.printStackTrace();
            }catch (Exception e){
                Log.e("EagerStrategy", e.getClass().getSimpleName()+ ": "+ e.getMessage());
            }

        }
    }
}
