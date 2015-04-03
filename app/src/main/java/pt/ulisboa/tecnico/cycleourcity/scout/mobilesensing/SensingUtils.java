package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing;

import edu.mit.media.funf.json.IJsonObject;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.MobileSensingException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.NoSuchSensorException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.SensorNotSupportedException;

/**
 * Created by rodrigo.jm.lourenco on 13/03/2015.
 */
public class SensingUtils {

    //FUNF PROBES
    public final static String ACCELEROMETER_PROBE = "\"edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe\"";
    public final static String GRAVITY_PROBE = "\"edu.mit.media.funf.probe.builtin.GravitySensorProbe\"";
    public final static String LOCATION_PROBE = "\"edu.mit.media.funf.probe.builtin.LocationProbe\"";
    public final static String BATTERY_PROBE = "\"edu.mit.media.funf.probe.builtin.BatteryProbe\"";

    //Sensor Types
    public final static int BATTERY         = 0;
    public final static int ACCELEROMETER   = 1;
    public final static int GRAVITY         = 2;
    public final static int LOCATION        = 3;


    //General Data Fields
    public final static String SENSOR_TYPE = "@type";
    public final static String CONFIG = "config";
    public final static String TIMESTAMP = "timestamp";
    public final static String NUM_FRAME_SAMPLES = "Total Samples";

    //Accelerometer Specific Fields
    public final static String ACCURACY = "accuracy";
    public final static String X = "x";
    public final static String Y = "y";
    public final static String Z = "z";

    //Location Specific Fields
    public static interface LocationKeys {
        public final static String
            ACCURACY    = "mAccuracy",
            ALTITUDE    = "mAltitude",
            LATITUDE    = "mLatitude",
            LONGITUDE   = "mLongitude",
            BEARING     = "mBearing",
            ELAPSED_TIME= "mElapsedRealtimeNanos",
            PROVIDER    = "mProvider",
            TIMESTAMP   = "timestamp";
    }

    //Configuration
    public final static int ACCELEROMETER_WINDOW_SIZE = 10;
    public static final double ACCELEROMETER_MAX_RATE = 100.0;

    public static boolean checkSensorType(String sensorType, IJsonObject sampleConfig){
        return sampleConfig.has(SENSOR_TYPE) && sampleConfig.get(SENSOR_TYPE).toString().equals(sensorType);
    }

    public static boolean isAccelerometerSample(IJsonObject config){
        return config.has(SENSOR_TYPE) && config.get(SENSOR_TYPE).toString().equals(ACCELEROMETER_PROBE);
    }


    /**
     * Given the sensor configuration, as an immutable JSON object, returns the sensor type.
     * @param sensorConfig the sensor's configuration
     * @return sensor type
     * @throws MobileSensingException if the configuration doesn't specify the sensor's name of if
     * the sensor is currently not supported by the application.
     */
    public static int getSensorType(IJsonObject sensorConfig) throws MobileSensingException{

        if(!sensorConfig.has(SENSOR_TYPE))
            throw new NoSuchSensorException();

        String sensor = String.valueOf(sensorConfig.get(SENSOR_TYPE));

        switch (sensor){
            case LOCATION_PROBE :
                return LOCATION;
            case ACCELEROMETER_PROBE :
                return ACCELEROMETER;
            case GRAVITY_PROBE:
                return GRAVITY;
            case BATTERY_PROBE:
                return BATTERY;
            default:
                throw new SensorNotSupportedException(sensor);
        }
    }

    public static String getSensorTypeAsString(int sensorType){

        switch (sensorType){
            case LOCATION:
                return "Location";
            case ACCELEROMETER:
                return "Accelerometer";
            case GRAVITY:
                return "Gravity";
            case BATTERY:
                return "Battery";
            default:
                return "Unknown Sensor";
        }

    }
}
