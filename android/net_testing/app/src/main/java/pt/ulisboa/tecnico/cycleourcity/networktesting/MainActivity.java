package pt.ulisboa.tecnico.cycleourcity.networktesting;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MainActivity extends ActionBarActivity {

    private Button wifiBtn, stopBtn;

    private ExecutorService service = Executors.newSingleThreadExecutor();
    private Future terminator;


    protected boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiBtn = (Button) findViewById(R.id.wifiBtn);

        stopBtn = (Button) findViewById(R.id.stopBtn);


        toggleButtons(true);

        wifiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                terminator = service.submit(new WifiTestTask());
                isRunning = true;
                toggleButtons(false);


            }
        });


        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.w("EXECUTOR", "Terminating running task...");

                terminator.cancel(true);
                service.shutdownNow();

                service = Executors.newSingleThreadExecutor();
                isRunning = false;

                if(!terminator.isCancelled())
                    Log.e("EXECUTOR", "Did not terminate...");

                toggleButtons(true);

            }
        });



    }


    private void toggleButtons(boolean enabled){
        wifiBtn.setEnabled(enabled);
        stopBtn.setEnabled(!enabled);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private abstract class TestTask {

        protected final JSONObject generateRandomData(int size){

            JSONObject data = new JSONObject();

            char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();

            StringBuilder sb = new StringBuilder();
            Random random = new Random();
            for (int i = 0; i < size; i++) {
                char c = chars[random.nextInt(chars.length)];
                sb.append(c);
            }

            String test = sb.toString();
            try {
                Log.w("TEST", "Actual size of the random string is "+test.getBytes("UTF-8").length+"KB");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            try {
                data.put("data", test);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return data;
        }
    }

    private class WifiTestTask extends TestTask implements Runnable{

        final String herokuURI = "https://quiet-beach-7008.herokuapp.com/myresource/test";

        private void uploadData(){

            final HttpPost httpost = new HttpPost(herokuURI);
            final DefaultHttpClient httpClient = new DefaultHttpClient();

            try {
                StringEntity entity = new StringEntity(String.valueOf(generateRandomData(100000)));
                httpost.setEntity(entity);
                httpost.setHeader("Accept", "application/json");
                httpost.setHeader("Content-type", "application/json");
                HttpResponse response = httpClient.execute(httpost);
                String result = EntityUtils.toString(response.getEntity());
                Log.d("WIFI", response.getStatusLine() + result);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            while(isRunning){

                Log.d("WIFI", "uploading data...");
                uploadData();

            }
        }
    }

    private class BluetoothTestTask extends TestTask implements Runnable {



        @Override
        public void run() {

        }
    }
}
