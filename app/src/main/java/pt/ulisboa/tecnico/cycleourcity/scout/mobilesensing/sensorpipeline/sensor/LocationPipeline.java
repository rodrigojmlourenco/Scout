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

    private static float MIN_ACCURACY = (float) 90.0;

    private static final SensorPipeline LOCATION_PIPELINE = new SensorPipeline();

    //Location Information Queues
    private Queue<JsonObject> sampleQueue;
    private Queue<JsonObject> extractedFeatures;

    //Logging
    private ScoutLogger logger = ScoutLogger.getInstance();

    public LocationPipeline(){

        this.sampleQueue = new LinkedList<>();
        this.extractedFeatures = new LinkedList<>();

        //Pre-processing pipeline stages
        LOCATION_PIPELINE.addStage(new AdmissionControlStage());
        LOCATION_PIPELINE.addStage(new TrimStage());
        LOCATION_PIPELINE.addFinalStage(new PostExecuteStage());
    }

    public static void setMinimumAccuracy(float minAccuracy){
        MIN_ACCURACY = minAccuracy;
    }

    public static float getMinimumAccuracy(){
        return MIN_ACCURACY;
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

    /****************************************************************************************
     * STAGES: Private stages to be used by the Location Pipeline                           *
     ****************************************************************************************/

    /**
     * In order to make the information captured by the location sensors this stage removes fields,
     * from the JsonObject that are irrelevant to the application.
     *
     * @see com.ideaimpl.patterns.pipeline.Stage
     */
    public class TrimStage implements Stage {

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

                trimmedSample.addProperty(SensingUtils.SENSOR_TYPE, SENSOR_TYPE); //TODO: descobrir como inserir isto sem quotes
                trimmedSample.addProperty(SensingUtils.LocationKeys.PROVIDER, provider);
                trimmedSample.addProperty(SensingUtils.LocationKeys.TIMESTAMP, timestamp);
                trimmedSample.addProperty(SensingUtils.LocationKeys.ACCURACY, accuracy);
                trimmedSample.addProperty(SensingUtils.LocationKeys.LATITUDE, latitude);
                trimmedSample.addProperty(SensingUtils.LocationKeys.LONGITUDE, longitude);
                trimmedSample.addProperty(SensingUtils.LocationKeys.ELAPSED_TIME, elapsedRealtimeNanos);
                trimmedSample.addProperty(SensingUtils.LocationKeys.BEARING, bearing);
                trimmedSample.addProperty(SensingUtils.LocationKeys.ALTITUDE, altitude);
                trimmedSample.addProperty(SensingUtils.LocationKeys.SPEED, speed);

                //Provider dependent data fields
                int providerType = getLocationProvider(sample);
                JsonObject extras = sample.get(SensingUtils.EXTRAS).getAsJsonObject();
                switch (providerType){
                    case GPS_PROVIDER:
                        satellites = extras.get(SensingUtils.LocationKeys.SATTELITES).getAsInt();
                        trimmedSample.addProperty(SensingUtils.LocationKeys.SATTELITES, satellites);
                        break;
                    case NETWORK_PROVIDER:
                        travelState = extras.get(SensingUtils.LocationKeys.TRAVEL_STATE).getAsString();
                        trimmedSample.addProperty(SensingUtils.LocationKeys.TRAVEL_STATE, travelState);
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
    public class AdmissionControlStage implements Stage {

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

            logger.log(ScoutLogger.INFO, LOG_TAG, TAG+discarded+" samples were discarded.");

            JsonObject[] output = new JsonObject[aux.size()];
            aux.toArray(output);

            //Pass results onto the next stage
            ((SensorPipeLineContext) pipelineContext).setInput(output); //For the next Stage
        }
    }

    /**
     *
     */
    public class FeatureExtraction implements Stage {

        @Override
        public void execute(PipelineContext pipelineContext) {
            //TODO: definir quais as features a extrair
        }
    }


    /**
     * @version 1.0
     * @author rodrigo.jm.lourenco
     *
     * Given the results of the previous stages, this stage updates the application's internal
     * state.
     */
    public class UpdateScoutStateStage implements Stage {

        private ScoutState state = ScoutState.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {

            MotionState auxMotionState = state.getMotionState();
            LocationState auxLocationState = state.getLocationState();

            double currTimestamp = 0, auxTimestamp = 0;
            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();

            for(JsonObject sample : input) {
                try {

                    auxTimestamp =
                            SensingUtils.LocationSampleAcessor.getTimestamp(sample);
                } catch (NoSuchDataFieldException e) {
                    e.printStackTrace();
                }


                //Check if sample is the most recent
                if (currTimestamp < auxTimestamp) {
                    state.setTimestamp(auxTimestamp);

                    //Update Location State
                    try {
                        auxLocationState.setLatitude(SensingUtils.LocationSampleAcessor.getLatitude(sample));
                    } catch (NoSuchDataFieldException e) {
                        e.printStackTrace();
                    }

                    try {
                        auxLocationState.setLongitude(SensingUtils.LocationSampleAcessor.getLongitude(sample));
                    } catch (NoSuchDataFieldException e) {
                        e.printStackTrace();
                    }
                    try {
                        auxLocationState.setAltitude(SensingUtils.LocationSampleAcessor.getAltitude(sample));
                    } catch (NoSuchDataFieldException e) {
                        e.printStackTrace();
                    }

                    //TODO: setSlope

                    //Update Motion State
                    try {
                        auxMotionState.setSpeed(SensingUtils.LocationSampleAcessor.getSpeed(sample));
                    } catch (NoSuchDataFieldException e) {
                        e.printStackTrace();
                    }

                    try {
                        auxMotionState.setTravelState(SensingUtils.LocationSampleAcessor.getTravelState(sample));
                    } catch (NoSuchDataFieldException e) {
                        e.printStackTrace();
                    }
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
     */
    public class PostExecuteStage implements Stage {

        private ScoutStorageManager storage = ScoutStorageManager.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {

            logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG+"pre-processing terminated.");

            int storedFeatures = 0;
            JsonObject[] output = ((SensorPipeLineContext)pipelineContext).getInput();

            for(JsonObject feature : output){

                extractedFeatures.add(feature);

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
