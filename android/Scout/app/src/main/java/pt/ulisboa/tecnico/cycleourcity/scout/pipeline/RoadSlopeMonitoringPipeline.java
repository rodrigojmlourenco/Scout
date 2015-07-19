package pt.ulisboa.tecnico.cycleourcity.scout.pipeline;

import android.util.Log;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.location.LocationUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain.StatisticalMetrics;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.PipelineConfiguration;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.ConfigurationCaretaker;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.SensorProcessingPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.stages.CommonStages;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.EvaluationSupportStorage;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager;

/**
 * Created by rodrigo.jm.lourenco on 18/07/2015.
 */
public class RoadSlopeMonitoringPipeline extends SensorProcessingPipeline {

    //Logging
    private static boolean VERBOSE = false;
    public final static String LOG_TAG = "RoadSlopeMonitoring";

    private RoadSlopeMonitoringState state;

    public RoadSlopeMonitoringPipeline(ConfigurationCaretaker caretaker) {
        super(SensingUtils.PRESSURE, caretaker);
        this.state = new RoadSlopeMonitoringState();
    }


    @Override
    public void pushSample(JsonObject sensorSample) {

        if(state.getPreviousState()!=null)
            sensorSample.add(SensingUtils.PressureKeys.PREVIOUS_PRESSURE, state.getPreviousState());

        super.pushSample(sensorSample);

    }

    @Override
    public void run() {
        super.run();

        if(!extractedFeaturesQueue.isEmpty())
            state.update(extractedFeaturesQueue.remove());
    }

    public static PipelineConfiguration generateRoadSlopeMonitoringPipelineConfiguration() {

        ScoutStorageManager storage = ScoutStorageManager.getInstance();

        PipelineConfiguration configuration = new PipelineConfiguration();

        configuration.addStage(new RoadSlopeMonitoringStages.ValidationStage());
        configuration.addStage(new RoadSlopeMonitoringStages.StoreRawValuesStage());
        //configuration.addStage(new RoadSlopeMonitoringStages.LowPassFilterStage()); //TODO
        //configuration.addStage(new RoadSlopeMonitoringStages.StoreFilteredValuesStage());
        configuration.addStage(new RoadSlopeMonitoringStages.MergeSamplesStage());
        configuration.addStage(new RoadSlopeMonitoringStages.DeriveAltitudeStage());
        configuration.addStage(new RoadSlopeMonitoringStages.DeriveSlopeStage());
        configuration.addStage(new RoadSlopeMonitoringStages.StoreFeatureVectorStage());

        configuration.addFinalStage(new RoadSlopeMonitoringStages.UpdateInnerStateStage());
        configuration.addFinalStage(new CommonStages.FeatureStorageStage(storage));

        return configuration;
    }

    /*
     ************************************************************************
     * Road Slope Monitoring Pipeline Stage                                 *
     ************************************************************************
     */
    public static interface RoadSlopeMonitoringStages {


        public class ValidationStage implements Stage {



            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                List<JsonObject> validSamples = new ArrayList<>();
                for(JsonObject sample : input){
                    if( sample.has(SensingUtils.LocationKeys.LOCATION) &&
                        sample.get(SensingUtils.LocationKeys.LOCATION) instanceof JsonObject)
                        validSamples.add(sample);
                }

                if(validSamples.isEmpty()){
                    if(VERBOSE) Log.w(LOG_TAG, "Scout is not fixed to a location yet! Skipping the next stages...");
                    ctx.addError("Scout is not fixed to a location yet!");
                    ctx.setInput(null);
                }
                else{
                    JsonObject[] validatedInput = new JsonObject[validSamples.size()];
                    validSamples.toArray(validatedInput);

                    ctx.setInput(validatedInput);
                }
            }
        }

        public class LowPassFilterStage implements Stage {

            @Override
            public void execute(PipelineContext pipelineContext) {

            }
        }

        public class MergeSamplesStage implements Stage {

            public JsonObject mergeSamples(double[] pressures, JsonObject location, JsonObject prevPressure) {

                JsonObject mergedSample = new JsonObject();
                double averagePressure = 0, variance = 0, stdDev = 0;
                int samplingSize = pressures.length;
                String locationTimestamp = location.get(SensingUtils.TIMESTAMP).getAsString();

                stdDev = StatisticalMetrics.calculateMean(pressures);
                variance = StatisticalMetrics.calculateVariance(pressures);
                averagePressure = StatisticalMetrics.calculateMean(pressures);

                mergedSample.addProperty(SensingUtils.SENSOR_TYPE, SensingUtils.PRESSURE);
                mergedSample.addProperty(SensingUtils.TIMESTAMP, locationTimestamp);
                mergedSample.addProperty(SensingUtils.SCOUT_TIME, System.nanoTime());
                mergedSample.addProperty(SensingUtils.PressureKeys.PRESSURE, averagePressure);
                mergedSample.addProperty(SensingUtils.PressureKeys.VARIANCE, variance);
                mergedSample.addProperty(SensingUtils.PressureKeys.STDEV, stdDev);
                mergedSample.addProperty(SensingUtils.PressureKeys.SAMPLES, samplingSize);
                mergedSample.add(SensingUtils.PressureKeys.PREVIOUS_PRESSURE, prevPressure);
                mergedSample.add(SensingUtils.LocationKeys.LOCATION, location);

                if(VERBOSE) Log.d(LOG_TAG, samplingSize+" pressure samples have been merged into one.");

                return mergedSample;
            }

            @Override
            public void execute(PipelineContext pipelineContext) {

                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput(), output = new JsonObject[1];
                JsonObject location = null, prevPressure = null;

                if(input[0]== null) return;

                try {
                        //Essential
                        if (input[0].has(SensingUtils.LocationKeys.LOCATION))
                            location = (JsonObject) input[0].get(SensingUtils.LocationKeys.LOCATION);

                        if(location == null) return;

                        //Not so essential
                        if (input[0].has(SensingUtils.PressureKeys.PREVIOUS_PRESSURE))
                            prevPressure = (JsonObject) input[0].get(SensingUtils.PressureKeys.PREVIOUS_PRESSURE);

                } catch (ClassCastException e) {
                    e.printStackTrace();
                }

                double[] pressures = new double[input.length];

                for (int i = 0; i < input.length; i++)
                    pressures[i] = input[i].get(SensingUtils.PressureKeys.PRESSURE).getAsDouble();

                try {
                    output[0] = mergeSamples(pressures, location, prevPressure);

                    ctx.setInput(output);
                }catch (NullPointerException e){
                    Log.e("PFX", String.valueOf(input[0]));
                }
            }
        }

        public class DeriveAltitudeStage implements Stage {

            @Override
            public void execute(PipelineContext pipelineContext) {

                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject input = ctx.getInput()[0];


                if(input == null) return; //Avoid NullPointer

                float altitude = LocationUtils.getAltitude(
                        LocationUtils.PRESSURE_STANDARD_ATMOSPHERE,
                        input.get(SensingUtils.PressureKeys.PRESSURE).getAsFloat());

                input.addProperty(SensingUtils.PressureKeys.ALTITUDE, altitude);

                if(VERBOSE) Log.d(LOG_TAG, "Altitude has been successfully derived.");
            }
        }

        public class DeriveSlopeStage implements Stage {

            @Override
            public void execute(PipelineContext pipelineContext) {

                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject currentSample = ctx.getInput()[0], prevSample;

                //Avoid NullPointer Exception
                if (currentSample == null) return;

                JsonObject from, to;
                double fromLon, fromLat, toLon, toLat, travelledDistance, fromAlt, toAlt, slope;


                //TODO: refractorizar isto !!!
                //CHECK: there is a previous sample

                try {
                    prevSample = (JsonObject) currentSample.get(SensingUtils.PressureKeys.PREVIOUS_PRESSURE);
                }catch (ClassCastException  e){
                    return;
                }

                if( currentSample.has(SensingUtils.PressureKeys.PREVIOUS_PRESSURE) &&
                    prevSample!=null){

                    //FETCH: from and to altitudes
                    fromAlt = prevSample.get(SensingUtils.PressureKeys.ALTITUDE).getAsDouble();
                    toAlt   = currentSample.get(SensingUtils.PressureKeys.ALTITUDE).getAsDouble();

                    //CHECK: both the current and previous samples are associated with locations
                    if( (to = (JsonObject) currentSample.get(SensingUtils.LocationKeys.LOCATION))!= null &&
                        (from = (JsonObject) prevSample.get(SensingUtils.LocationKeys.LOCATION))!=null){

                        //FETCH: latitude and longitude from both the from and to locations
                        fromLat = from.get(SensingUtils.LocationKeys.LATITUDE).getAsDouble();
                        fromLon = from.get(SensingUtils.LocationKeys.LONGITUDE).getAsDouble();
                        toLat   = to.get(SensingUtils.LocationKeys.LATITUDE).getAsDouble();
                        toLon   = to.get(SensingUtils.LocationKeys.LONGITUDE).getAsDouble();

                        //CALCULATE: travelled distance
                        travelledDistance = LocationUtils.calculateDistance(fromLat, fromLon, toLat, toLon);

                        //CALCULATE: slope
                        slope = LocationUtils.calculateSlope(travelledDistance, fromAlt, toAlt);

                        currentSample.addProperty(SensingUtils.PressureKeys.SLOPE, slope);
                        currentSample.addProperty(SensingUtils.PressureKeys.TRAVELLED_DISTANCE, travelledDistance);

                        //OPTIONAL: reordering for readability (location at the tail)
                        currentSample.remove(SensingUtils.LocationKeys.LOCATION);
                        currentSample.add(SensingUtils.LocationKeys.LOCATION, to);

                        if(VERBOSE) Log.d(LOG_TAG, "Slope has been successfully been derived.");
                    }
                }
            }
        }

        public class UpdateInnerStateStage implements Stage {

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;

                //Avoid NullPointer
                JsonObject output;
                if (ctx.getInput()==null || (output = ctx.getInput()[0])==null) return;

                output.remove(SensingUtils.PressureKeys.PREVIOUS_PRESSURE);

                ctx.setOutput(new JsonObject[]{output});

            }
        }

        public abstract class StoreValuesForTestStage implements Stage{

            private String testID;
            private EvaluationSupportStorage testStorage = EvaluationSupportStorage.getInstance();

            public StoreValuesForTestStage(String testID){
                this.testID = testID;
            }

            @Override
            public void execute(PipelineContext pipelineContext) {

                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                if(input == null) return;

                for(JsonObject sample : input)
                    if (sample != null) testStorage.storeSimplePressureTestValue(testID, sample);

            }
        }

        public class StoreRawValuesStage extends StoreValuesForTestStage {

            public StoreRawValuesStage() {
                super("raw");
            }
        }

        public class StoreFilteredValuesStage extends StoreValuesForTestStage {

            public StoreFilteredValuesStage() {
                super("filtered");
            }
        }

        public class StoreFeatureVectorStage implements Stage {

            private final String TEST_ID = "features";
            private EvaluationSupportStorage storage = EvaluationSupportStorage.getInstance();

            private final String TAG = "["+this.getClass().getSimpleName()+"]: ";

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                for(JsonObject featureVector : input)
                    if(featureVector != null)
                        storage.storeComplexPressureTestValue(TEST_ID, featureVector);
                    else if(VERBOSE)
                        Log.w(LOG_TAG, TAG+"No features to store");
            }
        }
    }

    /*
     ************************************************************************
     * Road Slope Monitoring Pipeline Internal Stage                        *
     ************************************************************************
     */

    public static class RoadSlopeMonitoringState {

        private JsonObject state = null;

        public void update(JsonObject sample){
            this.state = sample;
        }

        public JsonObject getPreviousState(){
            return this.state;
        }

    }
}
