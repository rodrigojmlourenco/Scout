package pt.ulisboa.tecnico.cycleourcity.evalscout.network.stages.strategies;

import android.util.Log;

import com.goebl.david.Response;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class BatchingStrategy implements UploadingStrategy{

    private final String LOG_TAG = this.getClass().getSimpleName();

    private final long MAX_SIZE;

    private long currentSize;
    public Queue<JsonObject> buffer;

    private final Object lock = new Object();

    public BatchingStrategy(int bufferMaxSize){
        MAX_SIZE = bufferMaxSize;
        buffer = new LinkedList<>();
    }

    private HttpResponse performBatchUpload(Queue<JsonObject> bufferSnap){

        JsonArray batchData = new JsonArray();

        for(JsonObject result : bufferSnap)
            batchData.add(result);

        //TESTING
        SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
        Log.d(LOG_TAG, format.format(new Date())+" ("+bufferSnap.size()+" samples) Sending ["+batchData+"]");

        HttpResponse response = uploadData(batchData);

        if(response != null){
            buffer.clear();
            currentSize=0;
        }

        return response;
    }

    private HttpResponse uploadData(JsonArray data){

        Gson gson = new Gson();
        HttpPost httpost = new HttpPost(herokuURI);
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = null;

        try {
            StringEntity entity = new StringEntity(gson.toJson(data));
            httpost.setEntity(entity);
            httpost.setHeader("Accept", "application/json");
            httpost.setHeader("Content-type", "application/json");
            response = httpClient.execute(httpost);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Upload failed, attempting again");
            response = uploadData(data);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    @Override
    public HttpResponse upload(JsonObject inference) {

        long objSize = String.valueOf(inference).getBytes(Charset.forName("UTF-8")).length;

        Queue<JsonObject> bufferSnap = null;

        synchronized (lock) {
            if ((currentSize + objSize) > MAX_SIZE)
                bufferSnap = new LinkedList(buffer);
            else {
                buffer.add(inference);
                currentSize += objSize;
                Log.d(LOG_TAG, (MAX_SIZE-currentSize)+"B to go");
            }
        }

        if(bufferSnap!=null)
            performBatchUpload(bufferSnap);

        return null;
    }
}
