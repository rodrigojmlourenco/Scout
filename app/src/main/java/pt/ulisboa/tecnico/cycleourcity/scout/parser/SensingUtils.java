package pt.ulisboa.tecnico.cycleourcity.scout.parser;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.json.IJsonObject;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.MobileSensingException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.NoSuchDataFieldException;
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
    public final static String SIMPLE_LOCATION_PROBE = "\"edu.mit.media.funf.probe.builtin.SimpleLocationProbe\"";

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
    public final static String EXTRAS = "mExtras";

    /**
     * Accelerometer Specific Fields */
    public static interface MotionKeys {

        public final static String X = "x";
        public final static String Y = "y";
        public final static String Z = "z";

        public final static String ACCURACY     = "accuracy";
        public final static String TIMESTAMP    = "timestamp";
        public final static String VERBOSE_TIMESTAMP = "dateTimestamp";

        public final static String ELAPSED_TIME = "elapsedTimeMillis";

        public final static String SAMPLES      = "numSamples";
    }

    /**
     * Location Specific Fields */
    public static interface LocationKeys {
        public final static String
            //Main Keys
            PROVIDER    = "mProvider",
            ACCURACY    = "mAccuracy",
            LATITUDE    = "mLatitude",
            LONGITUDE   = "mLongitude",
            ELAPSED_TIME= "mElapsedRealtimeNanos",
            TIMESTAMP   = "timestamp",
            TIME        = "mTime",
            //Only GPS
            BEARING     = "mBearing",
            ALTITUDE    = "mAltitude",
            SPEED       = "mSpeed",
            SATTELITES  = "satellites",
            //Only Network
            TRAVEL_STATE= "travelState",

            //Scout-specific Keys
            SLOPE       = "mSlope";
    }

    public static class LocationSampleAccessor{

        public static float getAccuracy(JsonObject sample){
            return sample.get(LocationKeys.ACCURACY).getAsFloat();
        }

        public static double getLatitude(JsonObject sample) {
            return sample.get(LocationKeys.LATITUDE).getAsDouble();

        }

        public static double getLongitude(JsonObject sample) {
            return sample.get(LocationKeys.LONGITUDE).getAsDouble();
        }

        public static float getAltitude(JsonObject sample) throws NoSuchDataFieldException {
            if(sample.has(LocationKeys.ALTITUDE)){
                return sample.get(LocationKeys.ALTITUDE).getAsFloat();
            }else
                throw new NoSuchDataFieldException(LocationKeys.ALTITUDE);
        }

        public static float getSpeed(JsonObject sample) throws NoSuchDataFieldException {
            if(sample.has(LocationKeys.SPEED))
                return sample.get(LocationKeys.SPEED).getAsFloat();
            else
                throw new NoSuchDataFieldException(LocationKeys.SPEED);
        }

        public static String getTravelState(JsonObject sample) throws NoSuchDataFieldException{
            if(sample.has(LocationKeys.TRAVEL_STATE))
                return sample.get(LocationKeys.TRAVEL_STATE).getAsString();
            else
                throw new NoSuchDataFieldException(LocationKeys.TRAVEL_STATE);
        }

        public static long getTimestamp(JsonObject sample) {
            return sample.get(LocationKeys.TIMESTAMP).getAsLong();
        }

        public static float getSlope(JsonObject sample) {
            return sample.get(LocationKeys.SLOPE).getAsFloat();
        }

        public static int getNumSatellites(JsonObject sample) throws NoSuchDataFieldException {

            /*JsonObject extras;

            if(sample.has(SensingUtils.EXTRAS))
                extras = sample.get(SensingUtils.EXTRAS).getAsJsonObject();
            else
                throw new NoSuchDataFieldException(LocationKeys.SATTELITES);

            if(extras != null && extras.has(LocationKeys.SATTELITES))
                return extras.get(LocationKeys.SATTELITES).getAsInt();
            else
                throw new NoSuchDataFieldException(LocationKeys.SATTELITES);
            */

            if(sample != null && sample.has(LocationKeys.SATTELITES))
                return sample.get(LocationKeys.SATTELITES).getAsInt();
            else
                throw new NoSuchDataFieldException(LocationKeys.SATTELITES);
        }

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
    public static int getSensorType(JsonObject sensorConfig) throws MobileSensingException{

        if(!sensorConfig.has(SENSOR_TYPE))
            throw new NoSuchSensorException();

        String sensor = String.valueOf(sensorConfig.get(SENSOR_TYPE));

        switch (sensor){
            case LOCATION_PROBE :
            case SIMPLE_LOCATION_PROBE:
                return LOCATION;
            case ACCELEROMETER_PROBE :
                return ACCELEROMETER;
            case GRAVITY_PROBE:
                return GRAVITY;
            case BATTERY_PROBE:
                return BATTERY;
            default:
                Log.e(sensor, String.valueOf(sensorConfig));
                throw new SensorNotSupportedException(sensor);
        }
    }

    /**
     * Given the sensor configuration, as an immutable JSON object, returns the sensor type.
     * @param sensorConfig the sensor's configuration
     * @return sensor type
     * @throws MobileSensingException if the configuration doesn't specify the sensor's name of if
     * the sensor is currently not supported by the application.
     */
    public static int getSensorType(IJsonObject sensorConfig) throws MobileSensingException{
        return getSensorType(sensorConfig.getAsJsonObject());
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