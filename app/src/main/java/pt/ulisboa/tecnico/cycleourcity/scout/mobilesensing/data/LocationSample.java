package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.data;

import edu.mit.media.funf.json.IJsonObject;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;

/**
 * Created by rodrigo.jm.lourenco on 26/03/2015.
 *
 * Sample Example:
 * "mAccuracy":43.0,
 * "mAltitude":271.0,
 * "mBearing":0.4,
 * "mElapsedRealtimeNanos":1211726188002399,
 * "mExtras":{"satellites":8},
 * "mHasAccuracy":true,
 * "mHasAltitude":true,
 * "mHasBearing":true,
 * "mHasSpeed":true,
 * "mIsFromMockProvider":false,
 * "mLatitude":38.73002376,
 * "mLongitude":-9.29588151,
 * "mProvider":"gps",
 * "mSpeed":24.0,
 * "mTime":1427384003000,
 * "timestamp":1427390663.514
 *
 * "mExtras":{"satellites":8},
 *
 * "mExtras":{"networkLocationType":"wifi","nlpVersion":2015,"noGPSLocation":{"mAccuracy":20.632,"mAltitude":0.0,"mBearing":0.0,"mElapsedRealtimeNanos":1218378457317756,"mExtras":{"networkLocationType":"wifi","nlpVersion":2015,"travelState":"stationary"},"mHasAccuracy":true,"mHasAltitude":false,"mHasBearing":false,"mHasSpeed":false,"mIsFromMockProvider":false,"mLatitude":38.7372053,"mLongitude":-9.3027706,"mProvider":"network","mSpeed":0.0,"mTime":1427390654884},"travelState":"stationary"},"mHasAccuracy":true,"mHasAltitude":false,"mHasBearing":false,"mHasSpeed":false,"mIsFromMockProvider":false,"mLatitude":38.7372053,"mLongitude":-9.3027706,"mProvider":"network","mSpeed":0.0,"mTime":1427390654884,"timestamp":1427390663.523}
 *
 * "mExtras":{
 *  "networkLocationType":"wifi",
 *  "nlpVersion":2015,
 *  "noGPSLocation":{
 *          "mAccuracy":20.632,
 *          "mAltitude":0.0,
 *          "mBearing":0.0,
 *          "mElapsedRealtimeNanos":1218378457317756,
 *          "mExtras":{
 *              "networkLocationType":"wifi",
 *              "nlpVersion":2015,
 *              "travelState":"stationary"}
*           ,"mHasAccuracy":true,
 *           "mHasAltitude":false,
 *           "mHasBearing":false,
 *           "mHasSpeed":false,
 *           "mIsFromMockProvider":false,
 *           "mLatitude":38.7372053,
 *           "mLongitude":-9.3027706,
 *           "mProvider":"network",
 *           "mSpeed":0.0,
 *           "mTime":1427390654884
 *       },
 *  "travelState":"stationary"},
 *"mHasAccuracy":true,
 * "mHasAltitude":false,
 * "mHasBearing":false,
 * "mHasSpeed":false,
 * "mIsFromMockProvider":false,
 * "mLatitude":38.7372053,
 * "mLongitude":-9.3027706,
 * "mProvider":"network",
 * "mSpeed":0.0,
 * "mTime":1427390654884,
 * "timestamp":1427390663.523}
 *
 * "mHasAccuracy":true,"mHasAltitude":true,"mHasBearing":true,"mHasSpeed":true,"mIsFromMockProvider":false,"mLatitude":38.73002376,"mLongitude":-9.29588151,"mProvider":"gps","mSpeed":24.0,"mTime":1427384003000,"timestamp":1427390663.514}
 */
public class LocationSample {

    private final static String LOG_TAG = "LocationSample";

    private final int sensorType = SensingUtils.ACCELEROMETER;
    private float accuracy;
    private float altitude;
    private double latitude;
    private double longitude;
    private float bearing;
    private double elapsedRealTimeMillis;
    private double timestamp;
    private int satellites;
    private String provider;
    private IJsonObject jsonSample;

    public LocationSample(IJsonObject data){

        provider = data.get(SensingUtils.LocationKeys.PROVIDER).getAsString();

        //Common Fields
        accuracy = data.get(SensingUtils.LocationKeys.ACCURACY).getAsFloat();
        altitude = data.has(SensingUtils.LocationKeys.ALTITUDE) ? data.get(SensingUtils.LocationKeys.ALTITUDE).getAsFloat() : 0;
        latitude = data.get(SensingUtils.LocationKeys.LATITUDE).getAsDouble();
        longitude= data.get(SensingUtils.LocationKeys.LONGITUDE).getAsDouble();
        bearing  = data.get(SensingUtils.LocationKeys.BEARING).getAsFloat();
        timestamp= data.get(SensingUtils.LocationKeys.TIMESTAMP).getAsDouble();
        elapsedRealTimeMillis = data.get(SensingUtils.LocationKeys.ELAPSED_TIME).getAsDouble()*0.000001;




    }

    public String getProvider() {
        return provider;
    }

    public int getSatellites() {
        return satellites;
    }
}
