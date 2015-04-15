package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import edu.mit.media.funf.json.IJsonObject;
import pt.ulisboa.tecnico.cycleourcity.scout.ScoutApplication;
import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.MobileSensingException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.NoSuchSensorException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor.AccelerometerPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor.LocationPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.parser.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.StorageManager;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.exceptions.NothingToArchiveException;


/**
 */
public class MobileSensingPipeline {

    public final static String  NAME = "Scout";
    public final static int     DB_VERSION = 1;
    private final static String LOG_TAG = "MobileSensingPipeline";

    public final static int     WINDOW_SIZE = 1; //seconds

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
    private SampleDispatcherTask dispatcher;

    //Storage
    private ScoutStorageManager storageManager = ScoutStorageManager.getInstance();
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
    }

    public static MobileSensingPipeline getInstance(){
        return SENSING_PIPELINE;
    }

    public void startSensingSession(){
        try {

            storageManager.clearStoredData();

            dispatcherSchedule = new Timer(true);
            dispatcher = new SampleDispatcherTask();

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
        JsonObject sample = sensorSample.getAsJsonObject();
        sample.addProperty(SensingUtils.SENSOR_TYPE, sensorType);

        synchronized (lock){ sensorSampleQueue.add(sample); }
    }

    /**
     *
     * @param sensorFeature
     */
    public void pushExtractedFeature(JsonObject sensorFeature) throws SQLException {
        extractedFeaturesQueue.add(sensorFeature);
    }

    /****************************************************************************************
     *  Async Work                                                                          *
     ****************************************************************************************/
    private class SampleDispatcherTask extends TimerTask {

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
                            accelerometerPipeline.pushSample(sample);
                            break;
                        case SensingUtils.GRAVITY:
                            accelerometerPipeline.pushSample(sample);
                            break;
                        case SensingUtils.LOCATION:
                            locationPipeline.pushSample(sample);
                            break;
                        default:
                            throw new NoSuchSensorException();
                    }
                } catch (MobileSensingException e) {
                    e.printStackTrace();
                }
            } while (sampleClone.peek() != null);

            //PRE-PROCESSING PHASE
            //locationPipeline.run();
            new Thread(locationPipeline).start();
            //accelerometerPipeline.run(); //TODO: uncomment
        }
    }




    /****************************************************************************************
     *                                          STORAGE                                     *
     ****************************************************************************************/
    public void archiveData(String tag) throws SQLException, NothingToArchiveException {

        StorageManager storage = ScoutStorageManager.getInstance();

        //TODO: check if there is information to store;
        storage.archive(tag);
        storage.archiveGPXTrack(tag);

        //Clear database contents
        storage.clearStoredData();

        Log.d(LOG_TAG, "[ARCHIVE]: " + "Stored samples successfully archived in the device's file system.");
    }
}
