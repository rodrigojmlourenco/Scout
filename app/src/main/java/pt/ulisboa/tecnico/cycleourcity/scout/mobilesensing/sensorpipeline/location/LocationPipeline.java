package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.location;


import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

import edu.mit.media.funf.json.IJsonObject;
import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeLineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor.FeatureExtractor;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor.ISensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.LocationState;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.ScoutState;
import pt.ulisboa.tecnico.cycleourcity.scout.parser.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.parser.exceptions.GPXBuilderException;
import pt.ulisboa.tecnico.cycleourcity.scout.parser.gpx.GPXBuilder;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager;

/**
 * Created by rodrigo.jm.lourenco on 24/04/2015.
 */
public class LocationPipeline implements ISensorPipeline {

    public final static String TAG = "[Location]";
    public final String LOG_TAG = this.getClass().getSimpleName();

    private final PressureSensorPipeline pressureSensorPipeline;
    private final LocationSensorPipeline locationSensorPipeline;

    private Queue<JsonObject> samplesQueue;

    private static SensorPipeline LOCATION_PIPELINE = new SensorPipeline();
    static {
        LOCATION_PIPELINE.addStage(new DispatchSensorSamplesStage());
        LOCATION_PIPELINE.addStage(new MergeStage());
        LOCATION_PIPELINE.addStage(new UpdateScoutStateStage());
        LOCATION_PIPELINE.addStage(new GPXBuildStage());
        LOCATION_PIPELINE.addFinalStage(new FeatureStorageStage());
    }

    private final Object lock = new Object();

    //Debugging
    private ScoutLogger logger = ScoutLogger.getInstance();

    public LocationPipeline(){

        this.pressureSensorPipeline = new PressureSensorPipeline();
        this.locationSensorPipeline = new LocationSensorPipeline();

        this.samplesQueue = new LinkedList<>();
    }


    @Override
    public void pushSample(IJsonObject sensorSample) {
        this.samplesQueue.add(sensorSample.getAsJsonObject());
    }

    @Override
    public void pushSample(JsonObject sensorSample) {
        this.samplesQueue.add(sensorSample);
    }

    @Override
    public void pushSampleCollection(Collection<JsonObject> sampleCollection) {
        this.samplesQueue.addAll(sampleCollection);
    }

    @Override
    public void run() {

        synchronized (lock) {
            logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG + "executing pipeline");

            JsonObject[] input = new JsonObject[samplesQueue.size()];
            samplesQueue.toArray(input);
            samplesQueue.clear();

            SensorPipeLineContext context = new SensorPipeLineContext();
            context.setInput(input);
            LOCATION_PIPELINE.execute(context);
        }
    }

    /****************************************************************************************
     * STAGES: Stages to be used by the Location Pipeline                                   *
     ****************************************************************************************/
    public static class DispatchSensorSamplesStage implements Stage{

        private ScoutLogger logger = ScoutLogger.getInstance();

        private LocationSensorPipeline locationSensorPipeline;
        private PressureSensorPipeline pressureSensorPipeline;

        @Override
        public void execute(PipelineContext pipelineContext) {

            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();

            Queue<JsonObject>   pressureSamples = new LinkedList<>(),
                                locationSamples = new LinkedList<>();

            int sensorType;
            for (JsonObject sample : input){

                sensorType = sample.get(SensingUtils.SENSOR_TYPE).getAsInt();

                switch (sensorType){
                    case SensingUtils.PRESSURE:
                        pressureSamples.add(sample);
                        break;
                    case SensingUtils.LOCATION:
                        locationSamples.add(sample);
                        break;
                    default:
                        logger.log(ScoutLogger.ERR, LocationPipeline.TAG, "Unsupported sensor type.");
                }
            }

            locationSensorPipeline = new LocationSensorPipeline();
            pressureSensorPipeline = new PressureSensorPipeline();

            locationSensorPipeline.pushSampleCollection(locationSamples);
            pressureSensorPipeline.pushSampleCollection(pressureSamples);

            Thread[] sensorTasks = new Thread[2];
            sensorTasks[0] = new Thread(locationSensorPipeline);
            sensorTasks[1] = new Thread(pressureSensorPipeline);

            sensorTasks[0].start();
            sensorTasks[1].start();

            for(Thread t : sensorTasks)
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            JsonObject[] pressureFeatures =
                    ((FeatureExtractor)pressureSensorPipeline).consumeExtractedFeatures();

            JsonObject[] locationFeatures =
                    ((FeatureExtractor)locationSensorPipeline).consumeExtractedFeatures();

            LinkedList<JsonObject> merger = new LinkedList<>();
            Collections.addAll(merger, pressureFeatures);
            Collections.addAll(merger, locationFeatures);

            JsonObject[] dispatchOutput = new JsonObject[merger.size()];
            merger.toArray(dispatchOutput);

            ((SensorPipeLineContext)pipelineContext).setInput(dispatchOutput);
        }
    }

    public static class MergeStage implements Stage {

        public final float nano2Millis = 1/1000000f;
        public final int MAX_TIME_DISTANCE = 1000;//ms

        private JsonObject mergeSamples(Queue<JsonObject> samples){

            JsonObject merged = new JsonObject();

            float pAltitude = -1f;
            JsonObject pressureSample = null;
            JsonObject locationSample = null;

            if (samples.size() > 2)
                logger.log(ScoutLogger.WARN, LOG_TAG, "Merging more than two samples, RISKY BUSINESS!");

            for(JsonObject sample : samples){

                switch (sample.get(SensingUtils.SENSOR_TYPE).getAsInt()){
                    case SensingUtils.LOCATION:
                        locationSample = sample;
                        break;
                    case SensingUtils.PRESSURE:
                        pressureSample = sample;
                        pAltitude = sample.get(SensingUtils.LocationKeys.BAROMETRIC_ALTITUDE).getAsFloat();
                        break;
                }
            }

            if(locationSample!=null && pAltitude!=-1) {
                locationSample.addProperty(SensingUtils.LocationKeys.BAROMETRIC_ALTITUDE, pAltitude);
                locationSample.add(SensingUtils.LocationKeys.PRESSURE, pressureSample);
                return locationSample;
            }

            return null;
        }

        private boolean closelyRelated(JsonObject sample1, JsonObject sample2){

            //Avoid NullPointerException
            if(sample1 == null || sample2 == null) return false;

            float t1, t2, elapsed;

            t1 = (SensingUtils.LocationSampleAccessor.getTimestamp(sample1)*nano2Millis);
            t2 = (SensingUtils.LocationSampleAccessor.getTimestamp(sample2)*nano2Millis);

            elapsed = Math.abs(t2-t1);

            return elapsed < MAX_TIME_DISTANCE;
        }

        private ScoutLogger logger = ScoutLogger.getInstance();
        private final String LOG_TAG = this.getClass().getSimpleName();

        @Override
        public void execute(PipelineContext pipelineContext) {
            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();

            if(input.length <= 0) return;

            int merged=0;
            JsonObject patientZero = input[0];
            Queue<JsonObject> samples2Merge = new LinkedList<>(),
                              mergedSamples = new LinkedList<>();

            for(JsonObject sample : input){
                if(closelyRelated(patientZero, sample)) {
                    samples2Merge.add(sample);
                    merged++;
                }else{
                    patientZero = sample;
                    mergedSamples.add(mergeSamples(samples2Merge));
                    samples2Merge.clear();
                }
            }

            if(!samples2Merge.isEmpty()) {
                mergedSamples.add(mergeSamples(samples2Merge));
                merged++;
            }

            logger.log(ScoutLogger.VERBOSE, LOG_TAG, "Merged "+merged+" samples into "+mergedSamples.size());


            JsonObject[] mergedOutput = new JsonObject[mergedSamples.size()];
            mergedSamples.toArray(mergedOutput);
            ((SensorPipeLineContext)pipelineContext).setInput(mergedOutput);
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

            LocationState locationState = state.getLocationState();

            double currTimestamp = 0, auxTimestamp;
            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();

            for(JsonObject sample : input) {

                if (sample!=null) {
                    auxTimestamp = SensingUtils.LocationSampleAccessor.getTimestamp(sample);
                    //Check if sample is the most recent
                    if (currTimestamp < auxTimestamp) {
                        state.setTimestamp(auxTimestamp);
                        locationState.updateLocationState(sample);
                    }
                }
            }
        }
    }

    /**
     * @version 1.2 Creates different maps for all different altitudes
     */
    public static class GPXBuildStage implements Stage {

        private final String LOG_TAG = this.getClass().getSimpleName();
        private GPXBuilder parser = GPXBuilder.getInstance();
        private ScoutLogger logger = ScoutLogger.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {

            if(!ScoutState.getInstance().isReady()){
                logger.log(ScoutLogger.WARN, LOG_TAG, "Scout is not ready, captured samples will not be stored.");
                return;
            }

            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();

            for(JsonObject location : input) {

                if(location!=null) { //TODO: assegurar que na Stage anterior nao passa NULL

                    try {
                        parser.addTrackPoint(GPXBuilder.GPS_BASED_ALTITUDE, location);
                    } catch (GPXBuilderException e) {
                        e.printStackTrace();
                    }

                    if (location.has(SensingUtils.LocationKeys.BAROMETRIC_ALTITUDE))
                        try {
                            parser.addTrackPoint(GPXBuilder.PRESSURE_BASED_ALTITUDE, location);
                        } catch (GPXBuilderException e) {
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
     *
     * TODO: esta stage deve ser igual para todos os pipelines pelo que pode ser externa
     */
    public static class FeatureStorageStage implements Stage {

        private final String LOG_TAG = this.getClass().getSimpleName();

        private ScoutLogger logger = ScoutLogger.getInstance();
        private ScoutStorageManager storage = ScoutStorageManager.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {

            if(!ScoutState.getInstance().isReady()){
                logger.log(ScoutLogger.WARN, LOG_TAG, "Scout is not ready, captured samples will not be stored.");
                return;
            }

            logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG+"pre-processing terminated.");

            int storedFeatures = 0;
            JsonObject[] output = ((SensorPipeLineContext)pipelineContext).getInput();

            for(JsonObject feature : output){

                if(feature!=null) { //TODO: assegurar que na Stage anterior nao passa NULL
                    //Persistent Storage
                    String key = feature.get(SensingUtils.SENSOR_TYPE).getAsString();
                    try {
                        storage.store(key, feature);
                        storedFeatures++;
                    } catch (SQLException e) {
                        e.printStackTrace();
                        logger.log(ScoutLogger.ERR, LOG_TAG, TAG + e.getMessage());
                    }
                }
            }

            logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG+storedFeatures+" were successfully stored.");

        }
    }
}
