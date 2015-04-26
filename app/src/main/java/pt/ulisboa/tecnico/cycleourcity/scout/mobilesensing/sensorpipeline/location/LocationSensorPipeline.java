package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.location;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

import edu.mit.media.funf.json.IJsonObject;
import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.NoSuchDataFieldException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeLineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.location.stages.HeuristicsAdmissionControlStage;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor.FeatureExtractor;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor.ISensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.stages.MergeStage;
import pt.ulisboa.tecnico.cycleourcity.scout.parser.SensingUtils;

/**
 * @version 1.3
 * @author rodrigo.jm.lourenco
 *
 *
 */
public class LocationSensorPipeline implements ISensorPipeline, FeatureExtractor {

    private final static String TAG         = "[LOCATION]: ";
    private final static String LOG_TAG     = "LocationPipeline";

    public final static int GPS_PROVIDER        = 0;
    public final static int NETWORK_PROVIDER    = 1;
    public final static int UNKNOWN_PROVIDER    = 2;

    private static final SensorPipeline LOCATION_PIPELINE = new SensorPipeline();

    //Location Information Queues
    private Queue<JsonObject> sampleQueue;
    private Queue<JsonObject> extractedFeaturesQueue;

    //Logging
    private ScoutLogger logger = ScoutLogger.getInstance();

    static {
        //Pre-processing pipeline stages
        LOCATION_PIPELINE.addStage(new TrimStage());
        LOCATION_PIPELINE.addStage(new MergeStage(new LocationSensorMergeStrategy()));
        LOCATION_PIPELINE.addStage(new HeuristicsAdmissionControlStage());
        LOCATION_PIPELINE.addFinalStage(new FeatureExtractionStage());
    }

    public LocationSensorPipeline(){
        this.sampleQueue = new LinkedList<>();
        this.extractedFeaturesQueue = new LinkedList<>();
    }

    @Override
    public void pushSample(JsonObject sensorSample) {
        this.sampleQueue.add(sensorSample);
    }

    @Override
    public void pushSampleCollection(Collection<JsonObject> sampleCollection) {
        this.sampleQueue.addAll(sampleCollection);
    }

    @Override
    public void pushSample(IJsonObject sensorSample) {
        pushSample(sensorSample.getAsJsonObject());
    }

    @Override
    public void run() {

        logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG+"executing pipeline");

        JsonObject[] input = new JsonObject[sampleQueue.size()];
        sampleQueue.toArray(input);
        sampleQueue.clear();

        SensorPipeLineContext context = new SensorPipeLineContext();
        context.setInput(input);
        LOCATION_PIPELINE.execute(context);

        JsonObject[] extractedFeatures = context.getOutput();
        Collections.addAll(this.extractedFeaturesQueue, extractedFeatures);

    }

    @Override
    public JsonObject[] consumeExtractedFeatures() {
        JsonObject[] features = new JsonObject[extractedFeaturesQueue.size()];
        extractedFeaturesQueue.toArray(features);
        extractedFeaturesQueue.clear();
        return  features;
    }


    /****************************************************************************************
     * STAGES: Private stages to be used by the Location Pipeline                           *
     ****************************************************************************************/

    /**
     * In order to make the information captured by the location sensors this stage removes fields,
     * from the JsonObject that are irrelevant to the application.
     *
     * @see com.ideaimpl.patterns.pipeline.Stage
     */
    public static class TrimStage implements Stage {

        private ScoutLogger logger = ScoutLogger.getInstance();

        /**
         * Given the location sample, as a JSON object, this method returns the location provider, which
         * may be either GPS or Network-based.
         * @param locationSample The location sample as a JsonObject
         * @return Location provider represented by an integer.
         */
        public int getLocationProvider(JsonObject locationSample){

            if(locationSample.has(SensingUtils.LocationKeys.PROVIDER)){
                String provider = locationSample.get(SensingUtils.LocationKeys.PROVIDER).getAsString();
                switch (provider) {
                    case SensingUtils.LocationKeys.GPS_PROVIDER:
                        return GPS_PROVIDER;
                    case SensingUtils.LocationKeys.NETWORK_PROVIDER:
                        return NETWORK_PROVIDER;
                }
            }

            return UNKNOWN_PROVIDER;
        }

        @Override
        public void execute(PipelineContext pipelineContext) {

            logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG+"trimming samples.");

            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();
            ArrayList<JsonObject> aux = new ArrayList<>();

            //Aux variables
            String  provider,
                    latitude,
                    longitude,
                    timestamp,
                    elapsedRealtimeNanos,
                    bearing,
                    accuracy,
                    altitude,
                    speed,
                    time,
                    satellites;

            String travelState;

            for(JsonObject sample : input){

                JsonObject trimmedSample = new JsonObject();

                //Main Data Fields
                provider = sample.get(SensingUtils.LocationKeys.PROVIDER).getAsString();
                accuracy = sample.get(SensingUtils.LocationKeys.ACCURACY).getAsString();
                latitude = sample.get(SensingUtils.LocationKeys.LATITUDE).getAsString();
                longitude = sample.get(SensingUtils.LocationKeys.LONGITUDE).getAsString();
                timestamp = sample.get(SensingUtils.LocationKeys.TIMESTAMP).getAsString();
                time      = sample.get(SensingUtils.LocationKeys.TIME).getAsString();
                elapsedRealtimeNanos = sample.get(SensingUtils.LocationKeys.ELAPSED_TIME_NANOS).getAsString();

                bearing = sample.get(SensingUtils.LocationKeys.BEARING).getAsString();
                altitude = sample.get(SensingUtils.LocationKeys.ALTITUDE).getAsString();
                speed = sample.get(SensingUtils.LocationKeys.SPEED).getAsString();

                trimmedSample.addProperty(SensingUtils.SENSOR_TYPE, SensingUtils.LOCATION);
                trimmedSample.addProperty(SensingUtils.LocationKeys.PROVIDER, provider);
                trimmedSample.addProperty(SensingUtils.LocationKeys.TIMESTAMP, timestamp);
                trimmedSample.addProperty(SensingUtils.LocationKeys.TIME, time);
                trimmedSample.addProperty(SensingUtils.LocationKeys.ACCURACY, accuracy);
                trimmedSample.addProperty(SensingUtils.LocationKeys.LATITUDE, latitude);
                trimmedSample.addProperty(SensingUtils.LocationKeys.LONGITUDE, longitude);
                trimmedSample.addProperty(SensingUtils.LocationKeys.ELAPSED_TIME_NANOS, elapsedRealtimeNanos);

                //Provider dependent data fields
                int providerType = getLocationProvider(sample);
                JsonObject extras = sample.get(SensingUtils.EXTRAS).getAsJsonObject();
                switch (providerType){
                    case GPS_PROVIDER:
                        trimmedSample.addProperty(SensingUtils.LocationKeys.BEARING, bearing);
                        trimmedSample.addProperty(SensingUtils.LocationKeys.ALTITUDE, altitude);
                        trimmedSample.addProperty(SensingUtils.LocationKeys.SPEED, speed);
                        if(extras != null){
                            satellites = extras.get(SensingUtils.LocationKeys.SATTELITES).getAsString();
                            trimmedSample.addProperty(SensingUtils.LocationKeys.SATTELITES, satellites);
                        }
                        break;
                    case NETWORK_PROVIDER:
                        if(extras != null) {
                            try {
                                travelState = extras.get(SensingUtils.LocationKeys.TRAVEL_STATE).getAsString();
                                trimmedSample.addProperty(SensingUtils.LocationKeys.TRAVEL_STATE, travelState);
                            }catch (NullPointerException e){
                                logger.log(ScoutLogger.DEBUG, LOG_TAG, "Network location sample has no travel state.");
                            }
                        }
                        break;
                    case UNKNOWN_PROVIDER:
                        logger.log(ScoutLogger.ERR, LOG_TAG, TAG+"Unknown location provider '"+provider+"'");
                }

                aux.add(trimmedSample);
            }

            JsonObject[] output = new JsonObject[aux.size()];
            aux.toArray(output);
            ((SensorPipeLineContext) pipelineContext).setInput(output); //For the next Stage
        }
    }

    /**
     * @version 0 Do Nothing
     * @author rodrigo.jm.lourenco
     */
    public static class FeatureExtractionStage implements Stage {

        private final static String LOG_TAG = "LOCATION_FeatureExtraction";

        //Application's Internal State
//        private LocationState appState = ScoutState.getInstance().getLocationState();

        //Logging
        private ScoutLogger logger = ScoutLogger.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {

            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();

            ((SensorPipeLineContext)pipelineContext).setOutput(input);

        }
    }



    private static class CustomCheckForRelationStrategy implements MergeStage.CheckForRelationStrategy {

        @Override
        public boolean areCloselyRelated(JsonObject sample1, JsonObject sample2) {

            //Avoid NullPointerException
            if(sample1 == null || sample2 == null) return false;

            BigDecimal b1 = new BigDecimal(sample1.get(SensingUtils.LocationKeys.ELAPSED_TIME_NANOS).getAsString());
            BigDecimal b2 = new BigDecimal(sample2.get(SensingUtils.LocationKeys.ELAPSED_TIME_NANOS).getAsString());

            BigDecimal e = b2.subtract(b1).multiply(MergeStage.DefaultCheckForRelation.SECOND_2_NANOS).abs();

            return (e.compareTo(MergeStage.DefaultCheckForRelation.MAX_TIME_FRAME)<= 0);

        }
    }


    /**
    * If two samples are considered to be closely related then they are merged according to the
    * following rules:
    * <ul>
    *     <li><i>timestamp</i>: the oldest of the two samples</li>
    *     <li>
    *         Given the sample with the highest precision then the new sample takes on the following
    *         values from that samples:
    *         <ul>
    *             <li><i>provider</i></li>
    *             <li><i>accuracy</i></li>
    *             <li><i>lat &amp; lon</i></li>
    *         </ul>
    *     </li>
    *     <li>
    *         The remaining fields are only important if the were generated by a GPS provider. These
    *         fields include:
    *         <ul>
    *             <li><i>speed</i></li>
    *             <li><i>bearing</i></li>
    *             <li><i>altitude</i></li>
    *             <li><i>satellites</i></li>
    *         </ul>
    *     </li>
    * </ul>
    */
    private static class LocationSensorMergeStrategy implements MergeStage.MergeStrategy {

        public void addGPSFields(JsonObject merged, JsonObject sample1, JsonObject sample2){

            int satellites=0;
            float acc1, acc2,
                    speed=0, bearing=0, altitude=0;


            String provider1, provider2;
            acc1 = SensingUtils.LocationSampleAccessor.getAccuracy(sample1);
            acc2 = SensingUtils.LocationSampleAccessor.getAccuracy(sample2);
            provider1 = sample1.get(SensingUtils.LocationKeys.PROVIDER).getAsString();
            provider2 = sample2.get(SensingUtils.LocationKeys.PROVIDER).getAsString();

            //Neither is GPS exit
            if(!provider1.equals(SensingUtils.LocationKeys.GPS_PROVIDER) &&
                    !provider2.equals(SensingUtils.LocationKeys.GPS_PROVIDER))
                return;



            //Both are from GPS then the one with the best accuracy wins;
            if(provider1.equals(SensingUtils.LocationKeys.GPS_PROVIDER) &&
                    provider2.equals(SensingUtils.LocationKeys.GPS_PROVIDER)){
                JsonObject aux = acc1 <= acc2 ? sample1 : sample2;
                speed = aux.get(SensingUtils.LocationKeys.SPEED).getAsFloat();
                bearing = aux.get(SensingUtils.LocationKeys.BEARING).getAsFloat();
                altitude = aux.get(SensingUtils.LocationKeys.ALTITUDE).getAsFloat();

                try {
                    satellites = SensingUtils.LocationSampleAccessor.getNumSatellites(aux);
                } catch (NoSuchDataFieldException e) {
                    satellites = 0;
                }

            }else{
                //Just one of the samples is from a GPS provider
                if(provider1.equals(SensingUtils.LocationKeys.GPS_PROVIDER)){ //Sample1 is from a GPS
                    speed = sample1.get(SensingUtils.LocationKeys.SPEED).getAsFloat();
                    bearing = sample1.get(SensingUtils.LocationKeys.BEARING).getAsFloat();
                    altitude = sample1.get(SensingUtils.LocationKeys.ALTITUDE).getAsFloat();

                    try {
                        satellites = SensingUtils.LocationSampleAccessor.getNumSatellites(sample1);
                    } catch (NoSuchDataFieldException e) {
                        satellites = 0;
                    }

                }else if (provider2.equals(SensingUtils.LocationKeys.GPS_PROVIDER)){ //Sample2 is from a GPS
                    speed = sample2.get(SensingUtils.LocationKeys.SPEED).getAsFloat();
                    bearing = sample2.get(SensingUtils.LocationKeys.BEARING).getAsFloat();
                    altitude = sample2.get(SensingUtils.LocationKeys.ALTITUDE).getAsFloat();

                    try {
                        satellites = SensingUtils.LocationSampleAccessor.getNumSatellites(sample2);
                    } catch (NoSuchDataFieldException e) {
                        satellites = 0;
                    }
                }
            }

            merged.addProperty(SensingUtils.LocationKeys.SPEED, speed);
            merged.addProperty(SensingUtils.LocationKeys.BEARING, bearing);
            merged.addProperty(SensingUtils.LocationKeys.ALTITUDE, altitude);
            merged.addProperty(SensingUtils.LocationKeys.SATTELITES, satellites);
        }


        private JsonObject mergeSamples(JsonObject sample1, JsonObject sample2){

            JsonObject merged = new JsonObject();

            //Avoid NullPointerException
            if(sample1 == null || sample2 == null) return null;

            float acc1, acc2, newAcc;
            acc1 = SensingUtils.LocationSampleAccessor.getAccuracy(sample1);
            acc2 = SensingUtils.LocationSampleAccessor.getAccuracy(sample2);

            String newProvider;
            double lat1, lon1, lat2, lon2, newLat, newLon;

            BigDecimal  timestamp1, timestamp2, nTimestamp,
                        elapsed1, elapsed2, nElapsed,
                        time1, time2, nTime;

            time1       = new BigDecimal(sample1.get(SensingUtils.LocationKeys.TIME).getAsString());
            time2       = new BigDecimal(sample2.get(SensingUtils.LocationKeys.TIME).getAsString());
            timestamp1  = new BigDecimal(sample1.get(SensingUtils.LocationKeys.TIMESTAMP).getAsString());
            timestamp2  = new BigDecimal(sample2.get(SensingUtils.LocationKeys.TIMESTAMP).getAsString());
            elapsed1    = new BigDecimal(sample1.get(SensingUtils.LocationKeys.ELAPSED_TIME_NANOS).getAsString());
            elapsed2    = new BigDecimal(sample2.get(SensingUtils.LocationKeys.ELAPSED_TIME_NANOS).getAsString());

            lat1 = sample1.get(SensingUtils.LocationKeys.LATITUDE).getAsDouble();
            lon1 = sample2.get(SensingUtils.LocationKeys.LONGITUDE).getAsDouble();
            lat2 = sample2.get(SensingUtils.LocationKeys.LATITUDE).getAsDouble();
            lon2 = sample2.get(SensingUtils.LocationKeys.LONGITUDE).getAsDouble();

            //Set the following fields depending on the accuracy:
            // - Provider
            // - Accuracy
            // - Lat & Lon (Average both)
            if(acc1 == acc2){
                BigDecimal two = new BigDecimal(2);
                newAcc = acc1;
                newProvider = sample1.get(SensingUtils.LocationKeys.PROVIDER).getAsString();
                newLat = (lat1 + lat2)/2;
                newLon = (lon1 + lon2)/2;
                nElapsed = (elapsed1.add(elapsed2)).divide(two);
                nTimestamp = (timestamp1.add(timestamp2)).divide(two);
                nTime = (time1.add(time2)).divide(two);
            }else {
                if (acc1 < acc2) { //Sample1 is better
                    newProvider = sample1.get(SensingUtils.LocationKeys.PROVIDER).getAsString();
                    newAcc = acc1;
                    newLat = lat1;
                    newLon = lon1;
                    nElapsed = elapsed1;
                    nTime = time1;
                    nTimestamp = timestamp1;
                } else {
                    newProvider = sample2.get(SensingUtils.LocationKeys.PROVIDER).getAsString();
                    newAcc = acc2;
                    newLat = lat2;
                    newLon = lon2;
                    nElapsed = elapsed2;
                    nTime = time2;
                    nTimestamp = timestamp2;
                }
            }

            //Add all the remaining fields
            merged.addProperty(SensingUtils.SENSOR_TYPE, SensingUtils.LOCATION);
            merged.addProperty(SensingUtils.LocationKeys.PROVIDER, newProvider);
            merged.addProperty(SensingUtils.TIMESTAMP, nTimestamp);
            merged.addProperty(SensingUtils.LocationKeys.TIME, nTime);
            merged.addProperty(SensingUtils.LocationKeys.ELAPSED_TIME_NANOS, nElapsed);
            merged.addProperty(SensingUtils.LocationKeys.ACCURACY, newAcc);
            merged.addProperty(SensingUtils.LocationKeys.LATITUDE, newLat);
            merged.addProperty(SensingUtils.LocationKeys.LONGITUDE, newLon);

            //Add GPS specific fields
            addGPSFields(merged, sample1, sample2);

            return merged;
        }

        @Override
        public JsonObject mergeSamples(Collection<JsonObject> samples) {

            JsonObject patientZero = null;
            for(JsonObject sample : samples){

                if(patientZero==null)
                    patientZero = sample;
                else{
                    patientZero = mergeSamples(patientZero, sample);
                }
            }

            //Debugging
            long time = System.currentTimeMillis();

            return patientZero;

        }
    }



}
