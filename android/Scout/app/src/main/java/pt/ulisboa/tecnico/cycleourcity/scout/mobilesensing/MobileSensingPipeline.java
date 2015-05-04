package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing;

import com.google.gson.JsonObject;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exceptions.MobileSensingException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exceptions.NoSuchSensorException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.location.LocationPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.motion.AccelerometerSensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.StorageManager;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.exceptions.NothingToArchiveException;


/**
 * The MobileSensingPipeline is one of Scout's core components. It is responsible for connecting
 * the sensor captured data and the respective sensor pre-processing pipelines.
 * <br>
 * The MobileSensingPipeline is designed as a singleton entity, meaning that at each moment
 * there is only one instance of the pipeline running, which is accessible to any Scout component.
 * <br>
 * This component is designed to run at a fixed rate, every few seconds, once a sensing session has
 * been initiated. At each iteration the MobileSensingPipeline dispatches all enqueue sensor samples
 * to a sensor specific pipeline responsible for processing those sample, and that runs asynchronously.
 * <br>
 * The MobileSensingPipeline also serves as a bridge between the application and the StorageManager.
 *
 * @author rodrigo.jm.lourenco
 * @version 1.0 Location&Pressure-Only
 */
public class MobileSensingPipeline {

    /**
     * Specifies the rate at which the MobileSensingPipeline sensing session should run,
     * that is, every WINDOW_SIZE seconds.
     */
    public final static int WINDOW_SIZE = 1;//seconds
    private final static String LOG_TAG = "MobileSensingPipeline";

    //Mobile Sensing Singleton
    private static MobileSensingPipeline SENSING_PIPELINE = new MobileSensingPipeline();

    //Sensor Specific Pipelines
    private final AccelerometerSensorPipeline accelerometerPipeline;
    private final LocationPipeline locationPipeline;
    //private final PressureSensorPipeline pressurePipeline;

    //Sensing Data Queues
    private Queue<JsonObject> sensorSampleQueue = null;
    private Queue<JsonObject> extractedFeaturesQueue = null;

    //Async Work
    private Object lock = new Object();
    private Timer dispatcherSchedule;
    private SampleDispatcherTask dispatcher;

    //Storage
    private ScoutStorageManager storageManager = ScoutStorageManager.getInstance();

    private MobileSensingPipeline() {

        //Sensor Pre-processing Pipelines
        this.accelerometerPipeline = new AccelerometerSensorPipeline();
        this.locationPipeline = new LocationPipeline();
        //this.pressurePipeline = new PressureSensorPipeline();

        //Sensing Data Queues
        this.sensorSampleQueue = new LinkedList<>();
        this.extractedFeaturesQueue = new LinkedList<>();
    }

    /**
     * Enables access to the MobileSensingPipeline singleton instance
     *
     * @return MobilePipelineSensing singleton
     */
    public static MobileSensingPipeline getInstance() {
        return SENSING_PIPELINE;
    }


    /**
     * Initiates a new sensing sensing, which executes at a fixed rate,
     * as specified by WINDOW_SIZE.
     */
    public void startSensingSession() {
        try {

            storageManager.clearStoredData();

            dispatcherSchedule = new Timer(true);
            dispatcher = new SampleDispatcherTask();

            dispatcherSchedule.scheduleAtFixedRate(dispatcher, 0, WINDOW_SIZE * 1000);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    /**
     * Terminates the sensing session.
     */
    public void stopSensingSession() {
        dispatcherSchedule.cancel();
        dispatcherSchedule.purge();
        dispatcher.cancel();
    }

    /**
     * Adds a new sensor data sample to the pre-processing queue.
     *
     * @param sensorSample The sensor data sample
     */
    public void pushSensorSample(JsonObject sensorSample) {
        synchronized (lock) {
            sensorSampleQueue.add(sensorSample);
        }
    }


    /*
     ****************************************************************************************
     * STORAGE                                                                              *
     * **************************************************************************************
     */

    /**
     * Given all the sensor data captured during the last sensing session, this method
     * procedes to storing persistently that information.
     * <br>
     * The information is stored in two ways:
     * <ul>
     * <li>
     * As a GPX file, which can be used to preview the traveled track.
     * </li>
     * <li>
     * As a DataBase file, which contains the information gathered by all running supported
     * sensors.
     * </li>
     * </ul>
     *
     * @param tag The prefix given to the file
     * @throws SQLException              should the ScoutStorageManager be unable to store the data.
     * @throws NothingToArchiveException should there be no information to store.
     * @see pt.ulisboa.tecnico.cycleourcity.scout.storage.StorageManager
     */
    public void archiveData(String tag) throws SQLException, NothingToArchiveException {

        StorageManager storage = ScoutStorageManager.getInstance();

        storage.archiveGPXTrack(tag);
        //TODO: check if there is information to store;
        storage.archive(tag);

        //Clear database contents
        storage.clearStoredData();
    }

    /*
     * *************************************************************************************
     * Async Work                                                                          *
     * **************************************************************************************
     */
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
            logger.log(ScoutLogger.VERBOSE, LOG_TAG, "Dispatching all " + sampleClone.size() + " samples...");
            do {
                JsonObject sample = (JsonObject) sampleClone.remove();

                try {
                    int sensorType = sample.get(SensingUtils.SENSOR_TYPE).getAsInt();
                    switch (sensorType) {
                        case SensingUtils.GRAVITY:
                        case SensingUtils.ACCELEROMETER:
                            accelerometerPipeline.pushSample(sample);
                            break;
                        case SensingUtils.PRESSURE:
                        case SensingUtils.LOCATION:
                        case SensingUtils.ORIENTATION:
                        case SensingUtils.ROTATION_VECTOR:
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
            new Thread(locationPipeline).start();
            //new Thread(accelerometerPipeline).start();
        }
    }
}