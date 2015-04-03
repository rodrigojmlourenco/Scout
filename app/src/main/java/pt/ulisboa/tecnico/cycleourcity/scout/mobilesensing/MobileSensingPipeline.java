package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.storage.DefaultArchive;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import edu.mit.media.funf.util.StringUtil;

import pt.ulisboa.tecnico.cycleourcity.scout.ScoutApplication;
import pt.ulisboa.tecnico.cycleourcity.scout.pipeline.ScoutPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.action.WriteDataAction;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.MobileSensingException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.NoSuchSensorException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor.AccelerometerPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.StorageManager;


/**
 */
public class MobileSensingPipeline {

    public final static String  NAME = "Scout";
    public final static int     DB_VERSION = 1;
    private final static String LOG_TAG = "MobileSensingPipeline";

    private boolean DEBUG = true;

    //Mobile Sensing Singleton
    private static MobileSensingPipeline SENSING_PIPELINE = new MobileSensingPipeline();


    //Actions
    private WriteDataAction writeAction;

    //Sensor Specific Pipelines
    private final AccelerometerPipeline accelerometerPipeline;


    //Sensing Data Queues
    private Queue<JsonObject> sensorSampleQueue = null;
    private Queue<JsonObject> extractedFeaturesQueue = null;

    //Storage
    private NameValueDatabaseHelper databaseHelper;
    private DefaultArchive archive;

    //Storage - Initial phase
    private Queue<JsonObject> storage = new LinkedList<>();

    private MobileSensingPipeline() {

        this.accelerometerPipeline = new AccelerometerPipeline();

        //Sensing Data Queues
        this.sensorSampleQueue = new LinkedList<>();
        this.extractedFeaturesQueue = new LinkedList<>();

        //Storage
        Context ctx = ScoutApplication.getContext();
        databaseHelper = new NameValueDatabaseHelper(ctx, StringUtil.simpleFilesafe(NAME), DB_VERSION);

        archive = new DefaultArchive(ctx, ScoutPipeline.NAME);
    }

    public static MobileSensingPipeline getInstance(){
        return SENSING_PIPELINE;
    }


    public void pushSensorSample(IJsonObject sensorConfig, IJsonObject sensorSample) throws MobileSensingException {

        int sensorType = SensingUtils.getSensorType(sensorConfig);


        //Store the sensor sample in a Queue
        //NOTE: por enquanto não tem qualquer função
        JsonObject sample = sensorSample.getAsJsonObject();
        sample.addProperty(SensingUtils.SENSOR_TYPE, sensorType);
        sample.addProperty(SensingUtils.CONFIG, sensorConfig.toString());
        sensorSampleQueue.add(sample);

        //TODO: remover esta linha
        storage.add(sample);

        switch (sensorType) {
            case SensingUtils.ACCELEROMETER:
                accelerometerPipeline.pushSample(sensorSample);
                break;
            case SensingUtils.LOCATION:
                //Log.d("LOCATION", String.valueOf(sensorSample));
                break;
            case SensingUtils.GRAVITY:
                //Log.d("GRAVITY", String.valueOf(sensorSample));
                break;
            default:
                throw new NoSuchSensorException();
        }
    }

    /**
     *
     * @param sensorFeature
     */
    public void pushExtractedFeature(JsonObject sensorFeature) throws SQLException {
        extractedFeaturesQueue.add(sensorFeature);
    }


    //TODO: make async
    public void archiveData() throws SQLException {

        if(storage.peek()==null) {
            Log.w("ARCHIVE", "Skipping, nothing to archive.");
            return;
        }


        int sampleCount = 0;
        StorageManager manager = ScoutStorageManager.getInstance();

        while(storage.peek()!=null){

            JsonObject value = storage.remove();
            int sensorType = value.get(SensingUtils.SENSOR_TYPE).getAsInt();
            String key = SensingUtils.getSensorTypeAsString(sensorType);

            Log.d("ARCHIVE", "Key:"+key+" - "+value.toString());

            try {
                manager.store(key, storage.remove());
            }catch (NoSuchElementException e){
                Log.e("ARCHIVE", "Something went terribly wrong.");
                return;
            }


            sampleCount++;
        }

        Log.d("ARCHIVE", "Storing " + sampleCount + " samples.");
        manager.archive();




    }

    public void setDebugLevel(int level){

        if(level > 0)
            DEBUG = true;
        else
            DEBUG = false;

    }
}
