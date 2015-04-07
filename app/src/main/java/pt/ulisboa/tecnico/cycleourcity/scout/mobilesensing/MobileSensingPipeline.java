package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.storage.DefaultArchive;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import edu.mit.media.funf.util.StringUtil;

import pt.ulisboa.tecnico.cycleourcity.scout.ScoutApplication;
import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor.LocationPipeline;
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

    public final static int     WINDOW_SIZE = 5; //seconds

    private boolean DEBUG = true;

    //Mobile Sensing Singleton
    private static MobileSensingPipeline SENSING_PIPELINE = new MobileSensingPipeline();


    //Sensor Specific Pipelines
    private final AccelerometerPipeline accelerometerPipeline;
    private final LocationPipeline locationPipeline;

    //Sensing Data Queues
    private Queue<JsonObject> sensorSampleQueue = null;
    private Queue<JsonObject> extractedFeaturesQueue = null;

    //Async Work
    private Object lock = new Object();
    private Timer dispatcherSchedule;
    private SampleDispatcher dispatcher = new SampleDispatcher();

    //Storage
    private NameValueDatabaseHelper databaseHelper;
    private DefaultArchive archive;
    private Queue<JsonObject> storage = new LinkedList<>();

    private MobileSensingPipeline() {

        //Sensor Pre-processing Pipelines
        this.accelerometerPipeline = new AccelerometerPipeline();
        this.locationPipeline = new LocationPipeline();

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

    public void startSensingSession(){
        try {
            dispatcherSchedule = new Timer(true);
            dispatcher = new SampleDispatcher();

            dispatcherSchedule.scheduleAtFixedRate(dispatcher, 0, WINDOW_SIZE * 1000);
        }catch (IllegalStateException e){
            e.printStackTrace();
        }
    }

    public void stopSensingSession(){
        dispatcherSchedule.cancel();
        dispatcherSchedule.purge();
        dispatcher.cancel();
    }

    public void pushSensorSample(IJsonObject sensorConfig, IJsonObject sensorSample)
            throws MobileSensingException {

        int sensorType = SensingUtils.getSensorType(sensorConfig);

        //Store the sensor sample in a Queue
        //NOTE: por enquanto não tem qualquer função
        JsonObject sample = sensorSample.getAsJsonObject();
        sample.addProperty(SensingUtils.SENSOR_TYPE, sensorType);
        //sample.addProperty(SensingUtils.CONFIG, sensorConfig.toString()); //Unnecessary
        sensorSampleQueue.add(sample);

        //TODO: remover esta linha
        //storage.add(sample);


        switch (sensorType) {
            case SensingUtils.ACCELEROMETER:
                accelerometerPipeline.pushSample(sensorSample);
                break;
            case SensingUtils.LOCATION:
                //locationPipeline.pushSample(sensorSample);
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

    public void setDebugLevel(int level){

        if(level > 0)
            DEBUG = true;
        else
            DEBUG = false;

    }

    /****************************************************************************************
     *  Async Work                                                                          *
     ****************************************************************************************
     *                                                                                      *
     ****************************************************************************************/
    private class SampleDispatcher extends TimerTask {

        ScoutLogger logger = ScoutLogger.getInstance();

        @Override
        public void run() {

            Queue sampleClone;

            logger.log(ScoutLogger.VERBOSE, LOG_TAG, "Starting new Pre-processing cycle...");

            //SNAPSHOT
            //Clone sensor sampling queue and then restart it.
            synchronized (lock) {
                if (sensorSampleQueue.peek() != null) {
                    sampleClone = new LinkedList(sensorSampleQueue);
                    sensorSampleQueue = new LinkedList<>();
                } else {
                    logger.log(ScoutLogger.ERR, LOG_TAG, "Sampling queue is empty");
                    return;
                }
            }

            //DISPATCH PHASE
            //Dispatch sensor samples for each specific sensor pipeline
            logger.log(ScoutLogger.VERBOSE, LOG_TAG, "Dispatching all "+sampleClone.size()+" samples...");
            do {
                JsonObject sample = (JsonObject) sampleClone.remove();

                try {
                    int sensorType = sample.get(SensingUtils.SENSOR_TYPE).getAsInt();
                    switch (sensorType) {
                        case SensingUtils.ACCELEROMETER:
                            //accelerometerPipeline.pushSample(sample);
                            break;
                        case SensingUtils.LOCATION:
                            locationPipeline.pushSample(sample);
                            break;
                        case SensingUtils.GRAVITY:
                            break;
                        default:
                            throw new NoSuchSensorException();
                    }
                } catch (MobileSensingException e) {
                    e.printStackTrace();
                }
            } while (sampleClone.peek() != null);

            //PRE-PROCESSING PHASE
            //Execute Pipelines
            //TODO: executar os pipelines especificos a cada sensor
            locationPipeline.run();
        }
    }




    /****************************************************************************************
     *                                          STORAGE                                     *
     ****************************************************************************************
     * TODO: make async
     ****************************************************************************************/
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
                break;
            }


            sampleCount++;
        }

        Log.d("ARCHIVE", "Storing " + sampleCount + " samples.");
        manager.archive();
    }

    public void archiveData(String tag) throws SQLException {

        StorageManager storage = ScoutStorageManager.getInstance();

        //TODO: check if there is information to store;
        storage.archive(tag);

        //Clear database contents
        storage.clearStoredData();

        Log.d(LOG_TAG, "[ARCHIVE]: "+"Stored samples successfully archived in the device's file system.");
    }
}
