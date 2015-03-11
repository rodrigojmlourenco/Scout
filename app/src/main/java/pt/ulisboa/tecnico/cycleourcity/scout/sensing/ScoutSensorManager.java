package pt.ulisboa.tecnico.cycleourcity.scout.sensing;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;

import com.ubhave.sensormanager.ESException;
import com.ubhave.sensormanager.ESSensorManager;
import com.ubhave.sensormanager.ESSensorManagerInterface;
import com.ubhave.sensormanager.config.SensorConfig;
import com.ubhave.sensormanager.config.pull.LocationConfig;
import com.ubhave.sensormanager.config.pull.MotionSensorConfig;
import com.ubhave.sensormanager.config.pull.PullSensorConfig;
import com.ubhave.sensormanager.sensors.SensorUtils;

import java.util.HashMap;
import java.util.Iterator;

import pt.ulisboa.tecnico.cycleourcity.scout.backend.ApplicationContext;
import pt.ulisboa.tecnico.cycleourcity.scout.sensing.config.ScoutSensorConfig;

/**
 * Created by rodrigo.jm.lourenco on 11/03/2015.
 *
 * ScoutSensorManager is the component responsible for managing the mobile device's sensors, as well
 * as gathering sensing data. This component is also responsible for activating and deactivating
 * sensors. Additionally it is responsible for managing the sensing sampling rates.
 *
 * This component is implemented as a singleton, in order to be accessible not only to the ScoutService
 * (@see pt.ulisboa.tecnico.cycleourcity.scout.backend.ScoutService) but also by all and any activity
 * that needs to fiddle with the sensors.
 */
public class ScoutSensorManager {

    //Debugging
    private final String LOG_TAG = "ScoutSensorManager";

    //Singleton
    private static ScoutSensorManager instance = null;

    //Sensors required by the application
    private final int[] scoutSensors = {
            //Core Sensors
            SensorUtils.SENSOR_TYPE_LOCATION

            /* TODO: enable
            SensorUtils.SENSOR_TYPE_ACCELEROMETER,
            SensorUtils.SENSOR_TYPE_MICROPHONE

            //SupportSensors
            SensorUtils.SENSOR_TYPE_LIGHT,
            SensorUtils.SENSOR_TYPE_GYROSCOPE,
            SensorUtils.SENSOR_TYPE_MAGNETIC_FIELD,
            SensorUtils.SENSOR_TYPE_WIFI,
            SensorUtils.SENSOR_TYPE_BATTERY,
            SensorUtils.SENSOR_TYPE_CONNECTION_STATE,
            SensorUtils.SENSOR_TYPE_CONNECTION_STRENGTH,
            SensorUtils.SENSOR_TYPE_PASSIVE_LOCATION,
            SensorUtils.SENSOR_TYPE_PROXIMITY,
            SensorUtils.SENSOR_TYPE_SCREEN,
            SensorUtils.SENSOR_TYPE_SMS,
            SensorUtils.SENSOR_TYPE_PHONE_STATE
            */
    };

    private final ESSensorManager xSensorManager;
    private HashMap<Integer, ScoutSensorDataListener> scoutSensorListeners;

    protected ScoutSensorManager() throws ESException {

        xSensorManager = ESSensorManager.getSensorManager(ApplicationContext.getContext());
        this.scoutSensorListeners = new HashMap<Integer, ScoutSensorDataListener>();

        for(int sensor : scoutSensors ){
            scoutSensorListeners.put(sensor, new ScoutSensorDataListener(sensor));
            adjustSensorConfigurationToScoutDefault(sensor);
        }
    }

    public static ScoutSensorManager getInstance() throws ESException {
        if(instance==null)
            instance = new ScoutSensorManager();

        return instance;
    }

    /**
     * Given all the sensors required by the application, this method subscribes to each sensor, thus
     * initiating the sensing data capture.
     * @throws ESException
     */
    public void startSensing() throws ESException {
        Log.d(LOG_TAG, "Subscribing to all sensors");
        for(int sensor : scoutSensors){
            scoutSensorListeners.get(sensor).subscribeToSensorData();
        }
    }


    /**
     * Given all the sensors required by the application, this method unsubscribes from each sensor,
     * this stopping the sensing data capture.
     * @throws ESException
     */
    public void stopSensing() throws ESException {
        Log.d(LOG_TAG, "Unsubscribing from all sensors");
        for(int sensor : scoutSensors){
            scoutSensorListeners.get(sensor).unsubscribeFromSensorData();
        }
    }

    /**
     * Pauses the sensing data capturing process for a given sensor.
     * @param sensor the sensor identification @see SensorUtils
     */
    public void pauseSensorSampling(int sensor) throws ESException {
        Log.d(LOG_TAG, "Pausing " + SensorUtils.getSensorName(sensor) + " sensing.");
        scoutSensorListeners.get(sensor).pauseSensorData();
    }

    /**
     * Unpauses the sensing data capturing process for a given sensor.
     * @param sensor
     */
    public void unpauseSensorSampling(int sensor) throws ESException {
        Log.d(LOG_TAG, "Resuming " + SensorUtils.getSensorName(sensor) + " sensing.");
        scoutSensorListeners.get(sensor).resumeSensorData();
    }

    /**
     * Adjusts the sensor sampling rate for a given sensor, but only if that sensor is a pull sensor.
     * @param sensor the sensor identification
     * @param rate sensing rate in millis
     * @throws ESException
     */
    public void adjustSensorSamplingRate(int sensor, int rate) throws ESException {
        Log.d(LOG_TAG, "Adjusting"+SensorUtils.getSensorName(sensor)+"'s sampling rate.");
        Log.w(LOG_TAG, "This method is not implemented");

        if(SensorUtils.isPullSensor(sensor))
            xSensorManager.setSensorConfig(sensor, PullSensorConfig.SENSE_WINDOW_LENGTH_MILLIS, rate);
        else
            Log.w(LOG_TAG, "Sensor sampling rates can only be adjusted for pull sensors.");

    }

    /**
     * Adjusts a sensor's configuration according to the defaults specified by the application
     * @param sensor the sensor identification
     * @throws ESException
     * @see pt.ulisboa.tecnico.cycleourcity.scout.sensing.config.ScoutSensorConfig
     */
    protected void adjustSensorConfigurationToScoutDefault(int sensor) throws ESException {
        SensorConfig config = ScoutSensorConfig.getDefaultConfig(sensor);
        adjustSensorConfiguration(sensor, config);
    }
    /**
     * Adjusts a sensor configuration according to the sensor configuration defined by config
     * @param sensor the sensor identification
     * @param config the object that defines the sensor configuration
     * @throws ESException
     * @see com.ubhave.sensormanager.config.SensorConfig
     * TODO: arranjar uma forma menos bruta de realizar isto, sem no entanto ter que modificar a lib
     */
    public void adjustSensorConfiguration(int sensor, SensorConfig config) throws ESException{

        Log.d(LOG_TAG, "Adjusting the sensor configuration.");

        if(SensorUtils.isPullSensor(sensor)) {

            if (config.containsParameter(LocationConfig.ACCURACY_TYPE))
                xSensorManager.setSensorConfig(sensor, LocationConfig.ACCURACY_TYPE,
                        config.getParameter(LocationConfig.ACCURACY_TYPE));

            if (config.containsParameter(PullSensorConfig.SENSE_WINDOW_LENGTH_MILLIS))
                xSensorManager.setSensorConfig(sensor, PullSensorConfig.SENSE_WINDOW_LENGTH_MILLIS,
                        config.getParameter(PullSensorConfig.SENSE_WINDOW_LENGTH_MILLIS));

            if(config.containsParameter(PullSensorConfig.POST_SENSE_SLEEP_LENGTH_MILLIS))
                xSensorManager.setSensorConfig(sensor,PullSensorConfig.POST_SENSE_SLEEP_LENGTH_MILLIS,
                        config.getParameter(PullSensorConfig.POST_SENSE_SLEEP_LENGTH_MILLIS));
        }
    }
}
