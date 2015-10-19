package pt.ulisboa.tecnico.cycleourcity.scout.senseless;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import pt.ulisboa.tecnico.cycleourcity.scout.ScoutApplication;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.MobileSensing;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.SensorProcessingPipeline;

/**
 * Created by rodrigo.jm.lourenco on 04/10/2015.
 */
public class SenselessEngine extends MobileSensing {

    public static final int DEFAULT_WINDOW_SIZE = 5;
    private static final String LOG_TAG = "SensingEngine";
    private LinkedList<Runnable> runnablePipelines = new LinkedList();
    private HashMap<Integer, SensorProcessingPipeline> pipelines = new HashMap();
    private Queue<JsonObject> sensorSampleQueue = null;
    private boolean isSensing = false;
    private int windowSize = 5;
    private Boolean asyncMode = Boolean.valueOf(true);
    private Object lock = new Object();
    private Timer dispatcherSchedule;
    private SenselessEngine.SenselessDispatchTask dispatchTask;

    public SenselessEngine(int windowSize){
        setWindowSize(windowSize);
    }

    @Override
    public void addSensorProcessingPipeline(SensorProcessingPipeline pipeline) {
        pipelines.put(pipeline.getSensorType(), pipeline);
        runnablePipelines.add(new Thread(pipeline));
    }

    @Override
    public void startSensingSession() {

        if(!this.isSensing) {
            dispatchTask = new SenselessDispatchTask();

            if(this.asyncMode.booleanValue()) {
                try {
                    this.dispatcherSchedule = new Timer(true);
                    this.dispatcherSchedule.scheduleAtFixedRate(dispatchTask, 0L, (long)(this.windowSize * 1000));
                } catch (IllegalStateException var2) {
                    var2.printStackTrace();
                }
            }

            this.isSensing = true;
        }
    }

    @Override
    public void stopSensingSession() {
        if(this.isSensing) {
            if(this.asyncMode.booleanValue()) {
                this.dispatcherSchedule.cancel();
                this.dispatcherSchedule.purge();
                this.dispatchTask.cancel();
            }

            this.isSensing = false;
        }

    }

    private class SenselessDispatchTask extends TimerTask {

        private Context context = ScoutApplication.getContext();
        private LinkedList<JsonObject> sampleQueueClone;

        public SenselessDispatchTask(){

            JsonParser parser = new JsonParser();
            sampleQueueClone = new LinkedList<>();
            File fakeDataFile = new File(Environment.getExternalStorageDirectory(), "My Documents/fake_data.txt");

            if(fakeDataFile==null || !fakeDataFile.exists()) {
                Log.e("FILES", "not found");
                return;
            }

            String line = new String();
            try {
                BufferedReader reader  = new BufferedReader(new FileReader(fakeDataFile));

                JsonObject fakeSample;
                while ((line = reader.readLine())!=null){
                    try {
                         fakeSample = (JsonObject) parser.parse(line);
                        sampleQueueClone.add(fakeSample);
                    }catch (Exception e ){
                        Log.e("DATA", line);
                    }

                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        private JsonParser parser = new JsonParser();
        private Gson gson = new Gson();
        public void run() {

            LinkedList<JsonObject> clone = new LinkedList();

            //Cloning Stage
            for(JsonObject cloneSample : this.sampleQueueClone){
                clone.add((JsonObject) parser.parse(gson.toJson(cloneSample)));
            }

            //DispatchPhase
            for(JsonObject sample : clone){
                int sensorType = sample.get(SensingUtils.GeneralFields.SENSOR_TYPE).getAsInt();

                if(!pipelines.containsKey(sensorType))
                    Log.e(LOG_TAG, SensingUtils.getSensorTypeAsString(sensorType)+" not supported");
                else
                    pipelines.get(sensorType).pushSample(sample);
            }

            for(Runnable runnable : runnablePipelines) {
                Thread t = new Thread(runnable);
                t.start();
            }
        }
    }
}
