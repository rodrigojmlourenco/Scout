package pt.ulisboa.tecnico.cycleourcity.scout.network.stages.strategies;

import android.util.Log;

import com.goebl.david.Response;
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
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BatchingStrategy implements UploadingStrategy{

    private final String LOG_TAG = this.getClass().getSimpleName();

    private final long MAX_SIZE;

    private long currentSize;
    public Queue<JsonObject> buffer;

    private final Object lock = new Object();

    private ExecutorService executor = Executors.newFixedThreadPool(3);

    public BatchingStrategy(int bufferMaxSize){
        MAX_SIZE = bufferMaxSize;
        buffer = new LinkedList<>();
    }

    private HttpResponse performBatchUpload(Queue<JsonObject> bufferSnap){
        executor.execute(new BatchUploadTask(bufferSnap));
        return null;
    }

    private HttpResponse uploadData(final JsonArray data){

        Gson gson = new Gson();
        final HttpPost httpost = new HttpPost(herokuURI);
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = null;

        try {
            StringEntity entity = new StringEntity(gson.toJson(data));
            httpost.setEntity(entity);
            httpost.setHeader("Accept", "application/json");
            httpost.setHeader("Content-type", "application/json");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        httpClient.execute(httpost);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();




        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    private EagerStrategy eager = new EagerStrategy();

    @Override
    public HttpResponse upload(JsonObject inference) {

        long objSize = String.valueOf(inference).getBytes(Charset.forName("UTF-8")).length;


        if(objSize > MAX_SIZE){
            Log.w(LOG_TAG, "Objects are too big, employing eager strategy");
            eager.upload(inference);
            return null;
        }

        Queue<JsonObject> bufferSnap = null;



        synchronized (lock) {

            if ((currentSize + objSize) > MAX_SIZE) {
                bufferSnap = new LinkedList(buffer);
                buffer.clear();
                currentSize = 0;
            }

            buffer.add(inference);
            currentSize += objSize;
        }

        if(bufferSnap!=null)
            performBatchUpload(bufferSnap);

        return null;
    }

    @Override
    public HttpResponse uploadRemaining() {
       synchronized (lock){
           if(currentSize >= 0){
               Queue<JsonObject> bufferSnap = new LinkedList(buffer);
               performBatchUpload(bufferSnap);
           }
       }

        return null;
    }

    private class BatchUploadTask implements Runnable{

        private final Gson gson = new Gson();
        private final JsonArray batchData;

        public BatchUploadTask(Queue<JsonObject> buffer){

            batchData = new JsonArray();

            for(JsonObject result : buffer)
                batchData.add(result);

            SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
            Log.d(LOG_TAG, format.format(new Date())+" ("+buffer.size()+" samples) Sending ["+batchData+"]");

        }

        @Override
        public void run() {

            final HttpPost httpost = new HttpPost(herokuURI);
            final DefaultHttpClient httpClient = new DefaultHttpClient();

            try {
                JsonObject data = new JsonObject();
                data.add("batch", batchData);

                StringEntity entity = new StringEntity(gson.toJson(data));
                httpost.setEntity(entity);
                httpost.setHeader("Accept", "application/json");
                httpost.setHeader("Content-type", "application/json");

                HttpResponse response = httpClient.execute(httpost);
                HttpEntity httpEntity = response.getEntity();
                String result = EntityUtils.toString(httpEntity);
                Log.d(LOG_TAG, response.toString()+": "+  result);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
