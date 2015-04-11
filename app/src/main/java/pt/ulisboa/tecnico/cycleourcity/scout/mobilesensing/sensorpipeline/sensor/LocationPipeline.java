package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor;

import com.google.gson.JsonObject;

import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import edu.mit.media.funf.json.IJsonObject;
import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.NoSuchDataFieldException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.location.LocationUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.LocationState;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.MotionState;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.ScoutState;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeLineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager;

public class LocationPipeline implements ISensorPipeline {

    private final static String TAG         = "[LOCATION]: ";
    private final static String LOG_TAG     = "LocationPipeline";
    private final static String SENSOR_TYPE = "Location";

    public final static int GPS_PROVIDER        = 0;
    public final static int NETWORK_PROVIDER    = 1;
    public final static int UNKNOWN_PROVIDER    = 2;

    public static final float MIN_ACCURACY = (float) 45.0;

    private static final SensorPipeline LOCATION_PIPELINE = new SensorPipeline();

    //Location Information Queues
    private Queue<JsonObject> sampleQueue;

    //Pipeline State
    private LocationState pipelineState;

    //Logging
    private ScoutLogger logger = ScoutLogger.getInstance();

    static {
        //Pre-processing pipeline stages
        LOCATION_PIPELINE.addStage(new AdmissionControlStage());
        LOCATION_PIPELINE.addStage(new TrimStage());
        LOCATION_PIPELINE.addStage(new MergeSamplesStage());
        LOCATION_PIPELINE.addStage(new FeatureExtractionStage());
        LOCATION_PIPELINE.addStage(new UpdateScoutStateStage());
        LOCATION_PIPELINE.addFinalStage(new PostExecuteStage());
    }

    public LocationPipeline(){
        this.sampleQueue = new LinkedList<>();
        this.pipelineState = new LocationState();
    }

    @Override
    public void pushSample(JsonObject sensorSample) {
        logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG+sensorSample);
        this.sampleQueue.add(sensorSample);
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
         * @param locationSample
         * @return Location provider represented by an integer.
         */
        public int getLocationProvider(JsonObject locationSample){

            if(locationSample.has(SensingUtils.LocationKeys.PROVIDER)){
                String provider = locationSample.get(SensingUtils.LocationKeys.PROVIDER).getAsString();
                switch (provider) {
                    case "gps":
                        return GPS_PROVIDER;
                    case "network":
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
            String provider;
            double  latitude,
                    longitude,
                    timestamp,
                    elapsedRealtimeNanos;

            float   bearing,
                    accuracy,
                    altitude,
                    speed;

            int satellites;

            String travelState;

            for(JsonObject sample : input){

                JsonObject trimmedSample = new JsonObject();

                //Main Data Fields
                provider = sample.get(SensingUtils.LocationKeys.PROVIDER).getAsString();
                accuracy = sample.get(SensingUtils.LocationKeys.ACCURACY).getAsFloat();
                latitude = sample.get(SensingUtils.LocationKeys.LATITUDE).getAsDouble();
                longitude = sample.get(SensingUtils.LocationKeys.LONGITUDE).getAsDouble();
                timestamp = sample.get(SensingUtils.LocationKeys.TIMESTAMP).getAsDouble();
                elapsedRealtimeNanos = sample.get(SensingUtils.LocationKeys.ELAPSED_TIME).getAsDouble();

                bearing = sample.get(SensingUtils.LocationKeys.BEARING).getAsFloat();
                altitude = sample.get(SensingUtils.LocationKeys.ALTITUDE).getAsFloat();
                speed = sample.get(SensingUtils.LocationKeys.SPEED).getAsFloat();

                trimmedSample.addProperty(SensingUtils.SENSOR_TYPE, SENSOR_TYPE);
                trimmedSample.addProperty(SensingUtils.LocationKeys.PROVIDER, provider);
                trimmedSample.addProperty(SensingUtils.LocationKeys.TIMESTAMP, timestamp);
                trimmedSample.addProperty(SensingUtils.LocationKeys.ACCURACY, accuracy);
                trimmedSample.addProperty(SensingUtils.LocationKeys.LATITUDE, latitude);
                trimmedSample.addProperty(SensingUtils.LocationKeys.LONGITUDE, longitude);
                trimmedSample.addProperty(SensingUtils.LocationKeys.ELAPSED_TIME, elapsedRealtimeNanos);

                //Provider dependent data fields
                int providerType = getLocationProvider(sample);
                JsonObject extras = sample.get(SensingUtils.EXTRAS).getAsJsonObject();
                switch (providerType){
                    case GPS_PROVIDER:
                        trimmedSample.addProperty(SensingUtils.LocationKeys.BEARING, bearing);
                        trimmedSample.addProperty(SensingUtils.LocationKeys.ALTITUDE, altitude);
                        trimmedSample.addProperty(SensingUtils.LocationKeys.SPEED, speed);
                        if(extras != null){
                            satellites = extras.get(SensingUtils.LocationKeys.SATTELITES).getAsInt();
                            trimmedSample.addProperty(SensingUtils.LocationKeys.SATTELITES, satellites);
                        }
                        break;
                    case NETWORK_PROVIDER:
                        if(extras != null) {
                            travelState = extras.get(SensingUtils.LocationKeys.TRAVEL_STATE).getAsString();
                            trimmedSample.addProperty(SensingUtils.LocationKeys.TRAVEL_STATE, travelState);
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
     * The information captured by the location sensors varies in quality. In order to assure the
     * application's robustness the AdmissionControl stage removes samples that may undermine the
     * quality of the system, for example samples with lower quality.
     *
     * @see com.ideaimpl.patterns.pipeline.Stage
     * @version 1.0
     */
    public static class AdmissionControlStage implements Stage {

        private ScoutLogger logger = ScoutLogger.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {

            //Logging
            logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG+"admission control.");

            int discarded = 0;
            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();
            Queue<JsonObject> aux = new LinkedList<>();

            for(JsonObject sample : input){

                float accuracy = sample.get(SensingUtils.LocationKeys.ACCURACY).getAsFloat();

                if(accuracy <= LocationPipeline.MIN_ACCURACY){
                    aux.add(sample);
                }else {
                    discarded++;
                    logger.log(
                            ScoutLogger.INFO,
                            LOG_TAG,
                            TAG + "Sample's accuracy bellow "+MIN_ACCURACY+". - (" + sample + ").");
                }
            }

            logger.log(ScoutLogger.INFO, LOG_TAG, TAG+discarded+" samples out of "+input.length+" were discarded.");

            JsonObject[] output = new JsonObject[aux.size()];
            aux.toArray(output);

            //Pass results onto the next stage
            ((SensorPipeLineContext) pipelineContext).setInput(output); //For the next Stage
        }
    }

    /**
     * TODO: esta stage precisa urgentemente de ser optimizada, demasiado código martelado.
     *
     * @version 1.0
     * @author rodrigo.jm.lourenco
     *
     * This stage is responsible for merging samples, before they are processed, that are considered
     * to be closely related, that is, samples that have ocurred in a time-window smaller the a
     * pre-defined sized window.
     *
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
     *         </ul>
     *     </li>
     * </ul>
     */
    public static class MergeSamplesStage implements Stage {

        private final static String LOG_TAG = "LOCATION_MergeSamples";

        public final long MAX_TIME_DISTANCE = 1000; //millis

        private ScoutLogger logger = ScoutLogger.getInstance();

        private boolean closelyRelated(JsonObject sample1, JsonObject sample2){

            //Avoid NullPointerException
            if(sample1 == null || sample2 == null) return false;

            long t1, t2, elapsed;

            t1 = SensingUtils.LocationSampleAccessor.getTimestamp(sample1);
            t2 = SensingUtils.LocationSampleAccessor.getTimestamp(sample2);

            elapsed = Math.abs(t2-t1);

            return elapsed < MAX_TIME_DISTANCE;
        }

        public void addGPSFields(JsonObject merged, JsonObject sample1, JsonObject sample2){
            float acc1, acc2;
            float speed=0, bearing=0, altitude=0;

            String provider1, provider2;
            acc1 = SensingUtils.LocationSampleAccessor.getAccuracy(sample1);
            acc2 = SensingUtils.LocationSampleAccessor.getAccuracy(sample2);
            provider1 = sample1.get(SensingUtils.LocationKeys.PROVIDER).getAsString();
            provider2 = sample2.get(SensingUtils.LocationKeys.PROVIDER).getAsString();

            //Neither is GPS exit
            //TODO: corrigir isto não devia estar hardcoded
            if(!provider1.equals("gps") && !provider2.equals("gps")) return;

            //Both are from GPS then the one with the best accuracy wins;
            if(provider1.equals("gps") && provider2.equals("gps")){
                JsonObject aux = acc1 <= acc2 ? sample1 : sample2;
                speed = aux.get(SensingUtils.LocationKeys.SPEED).getAsFloat();
                bearing = aux.get(SensingUtils.LocationKeys.BEARING).getAsFloat();
                altitude = aux.get(SensingUtils.LocationKeys.ALTITUDE).getAsFloat();
            }else{

                //Just one of the samples is from a GPS provider
                if(provider1.equals("gps")){ //Sample1 is from a GPS
                    speed = sample1.get(SensingUtils.LocationKeys.SPEED).getAsFloat();
                    bearing = sample1.get(SensingUtils.LocationKeys.BEARING).getAsFloat();
                    altitude = sample1.get(SensingUtils.LocationKeys.ALTITUDE).getAsFloat();
                }else if (provider2.equals("gps")){ //Sample2 is from a GPS
                    speed = sample2.get(SensingUtils.LocationKeys.SPEED).getAsFloat();
                    bearing = sample2.get(SensingUtils.LocationKeys.BEARING).getAsFloat();
                    altitude = sample2.get(SensingUtils.LocationKeys.ALTITUDE).getAsFloat();
                }
            }

            merged.addProperty(SensingUtils.LocationKeys.SPEED, speed);
            merged.addProperty(SensingUtils.LocationKeys.BEARING, bearing);
            merged.addProperty(SensingUtils.LocationKeys.ALTITUDE, altitude);

            return;
        }



        private JsonObject mergeSamples(JsonObject sample1, JsonObject sample2){

            JsonObject merged = new JsonObject();

            //Avoid NullPointerException
            if(sample1 == null || sample2 == null) return null;

            float acc1, acc2, newAcc;
            acc1 = SensingUtils.LocationSampleAccessor.getAccuracy(sample1);
            acc2 = SensingUtils.LocationSampleAccessor.getAccuracy(sample2);

            long t1, t2, newTimestamp;
            t1 = SensingUtils.LocationSampleAccessor.getTimestamp(sample1);
            t2 = SensingUtils.LocationSampleAccessor.getTimestamp(sample2);

            String newProvider;
            double lat1, lon1, lat2, lon2, newLat, newLon;


            //Set timestamp for the new sample
            newTimestamp = t1 < t2 ? t1 : t2;
            merged.addProperty(SensingUtils.SENSOR_TYPE, SENSOR_TYPE);
            merged.addProperty(SensingUtils.TIMESTAMP, newTimestamp);

            lat1 = sample1.get(SensingUtils.LocationKeys.LATITUDE).getAsDouble();
            lon1 = sample2.get(SensingUtils.LocationKeys.LONGITUDE).getAsDouble();
            lat2 = sample2.get(SensingUtils.LocationKeys.LATITUDE).getAsDouble();
            lon2 = sample2.get(SensingUtils.LocationKeys.LONGITUDE).getAsDouble();

            //Set the following fields depending on the accuracy:
            // - Provider
            // - Accuracy
            // - Lat & Lon (Average both)
            if(acc1 == acc2){
                newAcc = acc1;
                newProvider = sample1.get(SensingUtils.LocationKeys.PROVIDER).getAsString();
                newLat = (lat1 + lat2)/2;
                newLon = (lon1 + lon2)/2;
            }else {
                if (acc1 < acc2) { //Sample1 is better
                    newProvider = sample1.get(SensingUtils.LocationKeys.PROVIDER).getAsString();
                    newAcc = acc1;
                    newLat = lat1;
                    newLon = lon1;
                } else {
                    newProvider = sample2.get(SensingUtils.LocationKeys.PROVIDER).getAsString();
                    newAcc = acc2;
                    newLat = lat2;
                    newLon = lon2;
                }
            }

            //Add all the remaining fields
            merged.addProperty(SensingUtils.LocationKeys.PROVIDER, newProvider);
            merged.addProperty(SensingUtils.LocationKeys.ACCURACY, newAcc);
            merged.addProperty(SensingUtils.LocationKeys.LATITUDE, newLat);
            merged.addProperty(SensingUtils.LocationKeys.LONGITUDE, newLon);
            addGPSFields(merged, sample1, sample2);

            return merged;
        }

        @Override
        public void execute(PipelineContext pipelineContext) {

            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();
            Queue<JsonObject> mergedSamples = new LinkedList<>();
            int merged = 0;

            //Avoid further exceptions
            if(input.length <= 0) return;

            JsonObject auxSample = input[0];

            for(JsonObject sample : input)
                if(!auxSample.equals(sample))
                    if(closelyRelated(auxSample, sample)) {
                        auxSample = mergeSamples(auxSample, sample);
                        merged++;
                    }else{
                        mergedSamples.add(auxSample);
                        auxSample = sample;
                    }

            if(merged>0 && mergedSamples.isEmpty()) mergedSamples.add(auxSample);

            JsonObject[] output;
            if(!mergedSamples.isEmpty()){
                output = new JsonObject[mergedSamples.size()];
                mergedSamples.toArray(output);

                logger.log(ScoutLogger.INFO, LOG_TAG, input.length+" samples merged into "+output.length+".");

                ((SensorPipeLineContext)pipelineContext).setInput(output);

            }else{
                logger.log(ScoutLogger.INFO, LOG_TAG, "No samples were merged.");
            }
        }
    }

    /**
     * @version 1.1 Slope
     * @author rodrigo.jm.lourenco
     *
     *
     * Given the application's current state (given by the ScoutState singleton), this Stage calculates
     * calculates and adds the current slope value to the location samples.
     *
     */
    public static class FeatureExtractionStage implements Stage {

        private final static String LOG_TAG = "LOCATION_FeatureExtraction";

        //Application's Internal State
        private LocationState appState = ScoutState.getInstance().getLocationState();

        //Logging
        private ScoutLogger logger = ScoutLogger.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {

            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();

            double pastLat, pastLon, pastAlt;
            double currLat, currLon, currAlt=0;
            pastLat = appState.getLatitude();
            pastLon = appState.getLongitude();
            pastAlt = appState.getAltitude();

            for(JsonObject sample : input) {

                double distance, slope = 0;
                currLat = SensingUtils.LocationSampleAccessor.getLatitude(input[0]);
                currLon = SensingUtils.LocationSampleAccessor.getLongitude(input[0]);

                try {
                    currAlt = SensingUtils.LocationSampleAccessor.getAltitude(input[0]);
                } catch (NoSuchDataFieldException e) {
                    e.printStackTrace();
                }

                //Calculate Travelled Distance and Slope
                distance = LocationUtils.calculateDistance(pastLat, pastLon, currLat, currLon);

                if (distance > 0) {
                    slope = Math.abs(LocationUtils.calculateSlope(distance, pastAlt, currAlt));
                    sample.addProperty(SensingUtils.LocationKeys.SLOPE, slope);
                }else
                    sample.addProperty(SensingUtils.LocationKeys.SLOPE, 0);
            }

            ((SensorPipeLineContext)pipelineContext).setInput(input);

        }
    }


    /**
     * @version 1.0
     * @author rodrigo.jm.lourenco
     *
     * Given the results of the previous stages, this stage updates the application's internal
     * state.
     */
    public static class UpdateScoutStateStage implements Stage {

        private ScoutState state = ScoutState.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {

            MotionState auxMotionState = state.getMotionState();
            LocationState auxLocationState = state.getLocationState();

            double currTimestamp = 0, auxTimestamp = 0;
            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();

            for(JsonObject sample : input) {
                auxTimestamp = SensingUtils.LocationSampleAccessor.getTimestamp(sample);


                //Check if sample is the most recent
                if (currTimestamp < auxTimestamp) {
                    state.setTimestamp(auxTimestamp);

                    //Update Location State
                    auxLocationState.setLatitude(SensingUtils.LocationSampleAccessor.getLatitude(sample));
                    auxLocationState.setLongitude(SensingUtils.LocationSampleAccessor.getLongitude(sample));

                    try {
                        auxLocationState.setAltitude(SensingUtils.LocationSampleAccessor.getAltitude(sample));
                    } catch (NoSuchDataFieldException e) {}

                    auxLocationState.setSlope(SensingUtils.LocationSampleAccessor.getSlope(sample));

                    //Update Motion State
                    try {
                        auxMotionState.setSpeed(SensingUtils.LocationSampleAccessor.getSpeed(sample));
                    } catch (NoSuchDataFieldException e) {}

                    try {
                        auxMotionState.setTravelState(SensingUtils.LocationSampleAccessor.getTravelState(sample));
                    } catch (NoSuchDataFieldException e) {}
                }
            }
        }
    }

    /**
     * @version 1.0
     * This stage operates as a callback function, it extracts the output from the PipelineContext,
     * which is basically the extracted features, and stores it both in an extracted feature queue
     * and on the application's storage manager.
     *
     * @see pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeLineContext
     * @see pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager
     *
     * TODO: esta stage deve ser igual para todos os pipelines pelo que pode ser externa
     */
    public static class PostExecuteStage implements Stage {

        private ScoutLogger logger = ScoutLogger.getInstance();
        private ScoutStorageManager storage = ScoutStorageManager.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {

            logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG+"pre-processing terminated.");

            int storedFeatures = 0;
            JsonObject[] output = ((SensorPipeLineContext)pipelineContext).getInput();

            for(JsonObject feature : output){

                //Persistent Storage
                String key = feature.get(SensingUtils.SENSOR_TYPE).getAsString();
                try {
                    storage.store(key, feature);
                    storedFeatures++;
                } catch (SQLException e) {
                    e.printStackTrace();
                    logger.log(ScoutLogger.ERR, LOG_TAG, TAG+e.getMessage());
                }
            }

            logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG+storedFeatures+" were successfully stored.");
        }
    }
}
