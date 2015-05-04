package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing;

import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exceptions.MobileSensingException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exceptions.NoSuchDataFieldException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exceptions.NoSuchSensorException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exceptions.SensorNotSupportedException;

/**
 * Created by rodrigo.jm.lourenco on 13/03/2015.
 */
public class SensingUtils {

    //FUNF BUILTIN PROBES
    public final static String BATTERY_PROBE = "\"edu.mit.media.funf.probe.builtin.BatteryProbe\"";
    public final static String LOCATION_PROBE = "\"edu.mit.media.funf.probe.builtin.LocationProbe\"";
    public final static String GRAVITY_PROBE = "\"edu.mit.media.funf.probe.builtin.GravitySensorProbe\"";
    public final static String ORIENTATION_PROBE = "\"edu.mit.media.funf.probe.builtin.OrientationSensorProbe\"";
    public final static String SIMPLE_LOCATION_PROBE = "\"edu.mit.media.funf.probe.builtin.SimpleLocationProbe\"";
    public final static String ACCELEROMETER_PROBE = "\"edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe\"";
    public final static String ROTATION_VECTOR_PROBE = "\"edu.mit.media.funf.probe.builtin.RotationVectorSensorProbe\"";
    public final static String PRESSURE_PROBE = "\"edu.mit.media.funf.probe.builtin.PressureSensorProbe\"";



    //Sensor Types
    public final static int BATTERY         = 0;
    public final static int ACCELEROMETER   = 1;
    public final static int GRAVITY         = 2;
    public final static int LOCATION        = 3;
    public final static int ORIENTATION     = 4;
    public final static int ROTATION_VECTOR = 5;
    public final static int PRESSURE        = 6;


    //General Data Fields
    public final static String SENSOR_TYPE 			= "@type";
    public final static String CONFIG 				= "config";
    public final static String TIMESTAMP 			= "timestamp";
    public final static String NUM_FRAME_SAMPLES 	= "Total Samples";
    public final static String EXTRAS 				= "mExtras";
    public final static String ACCURACY     		= "accuracy";
    public final static String SCOUT_TIME           = "sTimestamp";

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
                PROVIDER                = "mProvider",
                ACCURACY                = "mAccuracy",
                LATITUDE                = "mLatitude",
                LONGITUDE               = "mLongitude",
                ELAPSED_TIME_NANOS      = "mElapsedRealtimeNanos",
                TIMESTAMP               = "timestamp",
                TIME                    = "mTime",

        //Only GPS
        BEARING                 = "mBearing",
                ALTITUDE                = "mAltitude",
                SPEED                   = "mSpeed",
                SATTELITES              = "satellites",

        //Only Network
        TRAVEL_STATE            = "travelState",

        //Pressure Sensor
        PRESSURE                = "pressure",
                BAROMETRIC_ALTITUDE     = "pAltitude",

        //Scout-specific Keys
        ELAPSED_TIME= "elapsedTime",
                SLOPE       = "mSlope",
                SAMPLES      = "numSamples",

        GPS_PROVIDER="gps",
                NETWORK_PROVIDER="network";
    }

    public static class LocationSampleAccessor{

        public static boolean isLocationSample(JsonObject sample){
            if(sample!=null)
                return sample.get(SENSOR_TYPE).getAsInt() == LOCATION;
            else
                return false;
        }

        public static float getAccuracy(JsonObject sample){
            try {
                return sample.get(LocationKeys.ACCURACY).getAsFloat();
            }catch (NullPointerException e){
                return 0;
            }
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

        public static float getBarometricAltitude(JsonObject sample) throws NoSuchDataFieldException {
            if(sample.has(LocationKeys.BAROMETRIC_ALTITUDE)){
                return sample.get(LocationKeys.BAROMETRIC_ALTITUDE).getAsFloat();
            }else
                throw new NoSuchDataFieldException(LocationKeys.BAROMETRIC_ALTITUDE);
        }

        public static float getSpeed(JsonObject sample) throws NoSuchDataFieldException {

            if(sample == null)
                throw new NoSuchDataFieldException(LocationKeys.SPEED);

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
            return new BigDecimal(sample.get(LocationKeys.TIMESTAMP).getAsString()).longValue();
        }

        public static float getSlope(JsonObject sample) {
            return sample.get(LocationKeys.SLOPE).getAsFloat();
        }

        public static int getNumSatellites(JsonObject sample) throws NoSuchDataFieldException {

            if(sample != null && sample.has(LocationKeys.SATTELITES))
                return sample.get(LocationKeys.SATTELITES).getAsInt();
            else
                throw new NoSuchDataFieldException(LocationKeys.SATTELITES);
        }

        public static long getElapsedRealTimeNanos(JsonObject sample){
            return sample.get(LocationKeys.ELAPSED_TIME_NANOS).getAsLong();
        }

        public static long getElapsedRealTimeMillis(JsonObject sample){
            long elapsedNanos = sample.get(LocationKeys.ELAPSED_TIME_NANOS).getAsLong();
            return TimeUnit.NANOSECONDS.convert(elapsedNanos, TimeUnit.MILLISECONDS);
        }

        public static long getTime(JsonObject sample){
            return sample.get(LocationKeys.TIME).getAsLong();
        }


    }

    //Configuration
    public final static int ACCELEROMETER_WINDOW_SIZE = 10;
    public static final double ACCELEROMETER_MAX_RATE = 100.0;

    public static boolean checkSensorType(String sensorType, JsonObject sampleConfig){
        return sampleConfig.has(SENSOR_TYPE) && sampleConfig.get(SENSOR_TYPE).toString().equals(sensorType);
    }

    public static boolean isAccelerometerSample(JsonObject config){
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
            case ORIENTATION_PROBE:
                return ORIENTATION;
            case ROTATION_VECTOR_PROBE:
                return ROTATION_VECTOR;
            case PRESSURE_PROBE:
                return PRESSURE;
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
            case ORIENTATION:
                return "Orientation";
            case ROTATION_VECTOR:
                return "RotationVector";
            case PRESSURE:
                return "Pressure";
            default:
                return "Unknown Sensor";
        }
    }
}