package pt.ulisboa.tecnico.cycleourcity.scout.pipeline;

import android.util.Log;

import com.google.gson.JsonNull;
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
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.SensorProcessingPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.stages.CommonStages;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.EvaluationSupportStorage;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.RouteStorage;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager;

/**
 * @version 1.0
 * @author rodrigo.jm.lourenco
 *
 * The RoadSlopeMonitoringPipeline is the pipeline responsible for calculating the slope of the
 * routes travelled by the participants.
 * <br>
 * In order to determine the slope of a given route this pipeline takes advantage of the fact
 * that it is possible to infer the altitude from the current pressure.
 * <br>
 * The infered altitude is not a real altitude, however because the ultimate purpose of this pipeline
 * it is to derive the slope there is no need for real altitudes.
 * <br>
 * <img src="http://www.satprepget800.com/wp-content/uploads/2013/11/Slope.png">
 */
public class RoadSlopeMonitoringPipeline extends SensorProcessingPipeline {

    private RoadSlopeMonitoringState state;

    public RoadSlopeMonitoringPipeline(PipelineConfiguration configuration) {
        super(SensingUtils.Sensors.PRESSURE, configuration);
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

    /**
     * Generates a pre-defined road slope monitoring oriented pipeline<br>
     * This configuration contemplates the following stages:<br>
     * <ol>
     *     <li>
     *         ValidationStage: checks if all samples possess all required fields, while discarding
     *         invalid samples.
     *         @see pt.ulisboa.tecnico.cycleourcity.scout.pipeline.RoadSlopeMonitoringPipeline.RoadSlopeMonitoringStages.ValidationStage
     *     </li>
     *     <li>
     *         LowPassFilter: applies a low-pass filter as to smooth the data and reduce the noise.
     *         TODO
     *         @see pt.ulisboa.tecnico.cycleourcity.scout.pipeline.RoadSlopeMonitoringPipeline.RoadSlopeMonitoringStages.LowPassFilterStage
     *     </li>
     *     <li>
     *         MergeSamplesStage: merges all the samples into a single sample by averaging all the
     *         pressure values.
     *         @see pt.ulisboa.tecnico.cycleourcity.scout.pipeline.RoadSlopeMonitoringPipeline.RoadSlopeMonitoringStages.MergeSamplesStage
     *     </li>
     *     <li>
     *         DeriveAltitudeStage: derives the altitude, of the current location, from the registered
     *         atmospheric pressure, as is performed natively by the Android's <a href="https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/hardware/SensorManager.java">SensorManager</a>.
     *         @see pt.ulisboa.tecnico.cycleourcity.scout.pipeline.RoadSlopeMonitoringPipeline.RoadSlopeMonitoringStages.DeriveAltitudeStage
     *     </li>
     *     <li>
     *         DeriveSlopeStage: derives the slope given the current and previous atmospheric pressures,
     *         as well as the current a previous locations (used to derice the distance travelled).
     *         @see pt.ulisboa.tecnico.cycleourcity.scout.pipeline.RoadSlopeMonitoringPipeline.RoadSlopeMonitoringStages.DeriveSlopeStage
     *     </li>
     * </ol>
     * <br>
     * Additionally and as to ease the evaluation stage, between stages that result in transformations
     * of the original sensor values there are stages that store those transformations. These files
     * may then be used to generate graphs as to better understand the impact of these stages.
     * @return RoadSlopeMonitoringPipeline's configuration
     */
    public static PipelineConfiguration generateRoadSlopeMonitoringPipelineConfiguration(boolean storeInfo) {

        ScoutStorageManager storage = ScoutStorageManager.getInstance();

        PipelineConfiguration configuration = new PipelineConfiguration();

        configuration.addStage(new RoadSlopeMonitoringStages.ValidationStage());
        if(storeInfo) configuration.addStage(new RoadSlopeMonitoringStages.StoreRawValuesStage());
        //configuration.addStage(new RoadSlopeMonitoringStages.LowPassFilterStage()); //TODO
        //configuration.addStage(new RoadSlopeMonitoringStages.StoreFilteredValuesStage());
        configuration.addStage(new RoadSlopeMonitoringStages.MergeSamplesStage());
        configuration.addStage(new RoadSlopeMonitoringStages.DeriveAltitudeStage());
        configuration.addStage(new RoadSlopeMonitoringStages.DeriveSlopeStage());
        if(storeInfo) configuration.addStage(new RoadSlopeMonitoringStages.StoreFeatureVectorStage());
        configuration.addStage(new RouteStorage.RouteStorageStage("barometric", RouteStorage.PRESSURE_BASED_ALTITUDE));

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

        /**
         * This stage is responsible for validating the pressure samples, where a sample
         * is said to be valid if it contains:
         * <ul>
         *     <li>Pressure values</li>
         *     <li>Location</li>
         * </ul>
         * Samples that do not conform to these requirements are discarded, and if all samples
         * are discarded then an error is pushed onto the pipeline, which will result in the
         * following stages to be skipped.
         */
        public class ValidationStage implements Stage {

            public boolean isValid(JsonObject sample){
                boolean valid = sample.has(SensingUtils.LocationKeys.LOCATION) &&
                        sample.get(SensingUtils.LocationKeys.LOCATION) instanceof JsonObject;

                if(valid){ //Invalidate if stationary
                    JsonObject location = (JsonObject) sample.get(SensingUtils.LocationKeys.LOCATION);
                    return  location.has(SensingUtils.LocationKeys.IS_STATIONARY) &&
                            !location.get(SensingUtils.LocationKeys.IS_STATIONARY).getAsBoolean();
                }else
                    return false;
            }

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                JsonObject location;
                List<JsonObject> validSamples = new ArrayList<>();
                for(JsonObject sample : input){
                    if(isValid(sample))
                        validSamples.add(sample);
                }

                if(validSamples.isEmpty()){
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

        //TODO: must be implemented
        public class LowPassFilterStage implements Stage {

            @Override
            public void execute(PipelineContext pipelineContext) {

            }
        }

        /**
         * This stage merges all the pressure samples into a single one. The new single sample
         * corresponds to the average value of all registered pressures for that frame.
         * <br>
         * In addition to that, some other fields are also added namely:<br>
         * <ol>
         *     <li>Variance</li>
         *     <li>Standard Deviation</li>
         * </ol>
         * <br>
         * The purpose of these new values is to better understand the overall relation of all
         * registered samples, e.g. the inherent noise.
         */
        public class MergeSamplesStage implements Stage {

            public JsonObject mergeSamples(double[] pressures, JsonObject location, JsonObject prevPressure) {

                JsonObject mergedSample = new JsonObject();
                double averagePressure = 0, variance = 0, stdDev = 0;
                int samplingSize = pressures.length;
                String locationTimestamp = location.get(SensingUtils.GeneralFields.TIMESTAMP).getAsString();

                stdDev = StatisticalMetrics.calculateMean(pressures);
                variance = StatisticalMetrics.calculateVariance(pressures);
                averagePressure = StatisticalMetrics.calculateMean(pressures);

                mergedSample.addProperty(SensingUtils.GeneralFields.SENSOR_TYPE, SensingUtils.Sensors.PRESSURE);
                mergedSample.addProperty(SensingUtils.GeneralFields.TIMESTAMP, locationTimestamp);
                mergedSample.addProperty(SensingUtils.GeneralFields.SCOUT_TIME, System.nanoTime());
                mergedSample.addProperty(SensingUtils.PressureKeys.PRESSURE, averagePressure);
                mergedSample.addProperty(SensingUtils.PressureKeys.VARIANCE, variance);
                mergedSample.addProperty(SensingUtils.PressureKeys.STDEV, stdDev);
                mergedSample.addProperty(SensingUtils.PressureKeys.SAMPLES, samplingSize);
                mergedSample.add(SensingUtils.PressureKeys.PREVIOUS_PRESSURE, prevPressure);
                mergedSample.add(SensingUtils.LocationKeys.LOCATION, location);

                return mergedSample;
            }

            @Override
            public void execute(PipelineContext pipelineContext) {

                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput(), output = new JsonObject[1];

                JsonObject  firstSample = input[0],
                        location = location = (JsonObject) input[0].get(SensingUtils.LocationKeys.LOCATION),
                        prevPressure = null;


                if (firstSample.has(SensingUtils.PressureKeys.PREVIOUS_PRESSURE) &&
                        firstSample.get(SensingUtils.PressureKeys.PREVIOUS_PRESSURE) instanceof JsonObject)
                    prevPressure = (JsonObject) firstSample.get(SensingUtils.PressureKeys.PREVIOUS_PRESSURE);


                double[] pressures = new double[input.length];

                for (int i = 0; i < input.length; i++)
                    pressures[i] = input[i].get(SensingUtils.PressureKeys.PRESSURE).getAsDouble();

                output[0] = mergeSamples(pressures, location, prevPressure);
                ctx.setInput(output);

            }
        }

        /**
         * Given a single sample, resulting from the merge operation, and its atmospheric pressure,
         * the sample's altitude is derived from that pressure.
         * <br>
         * Additionally this stage also performs some sort of validation by checking if the sample
         * is associated with a previously registed sample, which is essential to derive the slope.
         */
        public class DeriveAltitudeStage implements Stage {

            @Override
            public void execute(PipelineContext pipelineContext) {

                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject input = ctx.getInput()[0];

                float altitude = LocationUtils.getAltitude(
                        LocationUtils.PRESSURE_STANDARD_ATMOSPHERE,
                        input.get(SensingUtils.PressureKeys.PRESSURE).getAsFloat());

                input.addProperty(SensingUtils.PressureKeys.ALTITUDE, altitude);

                //Validate sample, as to avoid slope derivation error's
                if(!input.has(SensingUtils.PressureKeys.PREVIOUS_PRESSURE) ||
                        input.get(SensingUtils.PressureKeys.PREVIOUS_PRESSURE) instanceof JsonNull)
                    ctx.addError("Impossible to derive slope, as theres is no previous sample.");
            }
        }

        /**
         * Given the current sample, resulting from the merge operation, and previous, where both
         * are associated to a given location, the slope is derived from the distance travelled
         * between the two location and the altitudes differences.
         */
        public class DeriveSlopeStage implements Stage {

            @Override
            public void execute(PipelineContext pipelineContext) {

                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject currentSample = ctx.getInput()[0], prevSample;

                JsonObject from, to;
                double fromLon, fromLat, toLon, toLat, travelledDistance, fromAlt, toAlt, slope;

                prevSample = (JsonObject) currentSample.get(SensingUtils.PressureKeys.PREVIOUS_PRESSURE);

                //FETCH: from and to altitudes
                fromAlt = prevSample.get(SensingUtils.PressureKeys.ALTITUDE).getAsDouble();
                toAlt   = currentSample.get(SensingUtils.PressureKeys.ALTITUDE).getAsDouble();

                //CHECK: both the current and previous samples are associated with locations
                to = (JsonObject) currentSample.get(SensingUtils.LocationKeys.LOCATION);
                from = (JsonObject) prevSample.get(SensingUtils.LocationKeys.LOCATION);

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
            }
        }

        /**
         * This stage is required as to enable access to previously processed pressure samples,
         * which are necessary to derive the slope.
         */
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
