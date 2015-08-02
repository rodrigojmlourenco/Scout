package pt.ulisboa.tecnico.cycleourcity.scout.pipeline;

import android.opengl.Matrix;

import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.scout.calibration.ScoutCalibrationManager;
import pt.ulisboa.tecnico.cycleourcity.scout.calibration.exceptions.UninitializedException;
import pt.ulisboa.tecnico.cycleourcity.scout.learning.PavementType;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain.EnvelopeMetrics;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain.RMS;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain.StatisticalMetrics;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain.TimeDomainMetrics;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.PipelineConfiguration;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.SensorProcessingPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.stages.CommonStages;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.EvaluationSupportStorage;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.LearningSupportStorage;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager;

/**
 * @version 1.0 - Learning-Oriented
 * @author rodrigo.jm.lourenco
 * @see SensorProcessingPipeline
 *
 * The RoadConditionMonitoringPipeline is the pipeline responsible for determining the type and
 * condition of the surface currently being traversed by the participant. In order to do so this
 * pipeline will analyse sensor values originated from the Linear Acceleration sensor.
 *
 *
 */
public class RoadConditionMonitoringPipeline extends SensorProcessingPipeline {

    private ScoutCalibrationManager calibrationManager = null;
    public final static int SENSOR_TYPE = SensingUtils.Sensors.LINEAR_ACCELERATION;

    public RoadConditionMonitoringPipeline(PipelineConfiguration configuration) {
        super(SENSOR_TYPE, configuration);

        try{
            calibrationManager = ScoutCalibrationManager.getInstance();
        } catch (UninitializedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pushSample(JsonObject sensorSample) {

        if(calibrationManager != null)
            calibrationManager.tagLinearAccelerationOffsets(sensorSample);

        super.pushSample(sensorSample);
    }

    /**
     * Generates a pre-defined road condition monitoring oriented pipeline.<br>
     * This configuration contemplates the following stages:<br>
     * <ol>
     *     <li>
     *         ValidationStage: validates the samples, removing invalid ones.
     *         @see pt.ulisboa.tecnico.cycleourcity.scout.pipeline.RoadConditionMonitoringPipeline.RoadConditionMonitoringStages.ValidationStage
     *     </li>
     *     <li>
     *         NormalizationStage: normalizes the sample's values by removing an offset acquired
     *         during calibration.
     *         @see pt.ulisboa.tecnico.cycleourcity.scout.pipeline.RoadConditionMonitoringPipeline.RoadConditionMonitoringStages.NormalizationStage
     *     </li>
     *     <li>
     *         ProjectionStage: given a rotation matrix, this stage projects the sample's values
     *         onto the Earth's coordinate system which is an absolute coordinate system.
     *         @see pt.ulisboa.tecnico.cycleourcity.scout.pipeline.RoadConditionMonitoringPipeline.RoadConditionMonitoringStages.ProjectionStage
     *     </li>
     *     <li>
     *         OverkillFeatureExtractionStage: extracts a wide plethora of features (all time-domain).
     *         Although ultimetly there is no need for such a wide variety of features, because at
     *         this stage it is still not clear the most relevant ones, all possible features are
     *         computed and added to the feature vector.
     *         @see pt.ulisboa.tecnico.cycleourcity.scout.pipeline.RoadConditionMonitoringPipeline.RoadConditionMonitoringStages.OverkillZFeatureExtractionStage
     *     </li>
     *     <li>
     *         TagForLearningStage: tags the samples with a given type of pavement, this tag will then
     *         be used to classify the samples during the machine learning phase.
     *         @see pt.ulisboa.tecnico.cycleourcity.scout.pipeline.RoadConditionMonitoringPipeline.RoadConditionMonitoringStages.TagForLearningStage
     *     </li>
     * </ol>
     * <br>
     * Additionally and as to ease the evaluation stage, between stages that result in transformations
     * of the original sensor values there are stages that store those transformations. These files
     * may then be used to generate graphs as to better understand the impact of these stages.
     * @return RoadConditionMonitoringPipeline's configuration
     */
    public static PipelineConfiguration generateRoadConditionMonitoringPipelineConfiguration(){
        ScoutStorageManager storage = ScoutStorageManager.getInstance();

        PipelineConfiguration configuration = new PipelineConfiguration();

        configuration.addStage(new RoadConditionMonitoringStages.ValidationStage());

        configuration.addStage(new RoadConditionMonitoringStages.StoreRawValuesStage());            //TESTING

        configuration.addStage(new RoadConditionMonitoringStages.NormalizationStage());
        configuration.addStage(new RoadConditionMonitoringStages.StoreNormalizedValuesStage());     //TESTING

        configuration.addStage(new RoadConditionMonitoringStages.ProjectionStage());
        configuration.addStage(new RoadConditionMonitoringStages.StoreProjectedValuesStage());      //TESTING

        configuration.addStage(new RoadConditionMonitoringStages.OverkillZFeatureExtractionStage());//LEARNING
        configuration.addStage(new RoadConditionMonitoringStages.TagForLearningStage());            //LEARNING
        configuration.addStage(new RoadConditionMonitoringStages.StoreFeatureVectorStage());

        configuration.addFinalStage(new CommonStages.FeatureStorageStage(storage));
        configuration.addFinalStage(new RoadConditionMonitoringStages.FinalizeStage());

        return configuration;
    }

    /*
     ************************************************************************
     * Road Condition Monitoring Pipeline                                   *
     ************************************************************************
     */
    public static interface RoadConditionMonitoringStages{

        public final String LOG_TAG = "RoadConditionMonitoring";

        /**
         * This stage is responsible for validating the linear acceleration samples, where a sample
         * is said to be valid if it contains:
         * <ul>
         *     <li>Linear acceleration values</li>
         *     <li>Location</li>
         *     <li>Rotation Matrix</li>
         *     <li>Calibration offset</li>
         * </ul>
         * Samples that do not conform to these requirements are discarded, and if all samples
         * are discarded then an error is pushed onto the pipeline, which will result in the
         * following stages to be skipped.
         */
        public static class ValidationStage implements Stage {

            private boolean validSample(JsonObject sample){
                return sample.has(SensingUtils.MotionKeys.LOCATION) &&
                        ! (sample.get(SensingUtils.MotionKeys.LOCATION) instanceof JsonNull) &&
                        sample.has(SensingUtils.MotionKeys.ROTATION) &&
                        ! (sample.get(SensingUtils.MotionKeys.ROTATION) instanceof JsonNull) &&
                        sample.has(SensingUtils.MotionKeys.CALIBRATION) &&
                        ! (sample.get(SensingUtils.MotionKeys.CALIBRATION) instanceof JsonNull);
            }

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                List<JsonObject> validSamples = new ArrayList<>();

                for(JsonObject sample : ctx.getInput())
                    if(validSample(sample)) validSamples.add(sample);

                if(validSamples.isEmpty()) {
                    String error = "There are no complete samples in this iteration";
                    ctx.addError(error);
                }else{
                    JsonObject[] validated = new JsonObject[validSamples.size()];
                    validSamples.toArray(validated);
                    ctx.setInput(validated);
                }
            }
        }

        /**
         * Given the linear acceleration sensor offset values, which were acquired during a calibration
         * process, this stage normalizes the sensor's values by subtracting that offset.
         * <br>
         * This process is based upon the one described in <a href="http://bscw.wineme.fb5.uni-siegen.de/pub/bscw.cgi/S54d30e14/d802414/jigsaw.pdf">Jigsaw</a>.
         */
        public static class NormalizationStage implements Stage {

            private boolean normalizeSample(JsonObject sample){

                JsonObject offsets =
                        (JsonObject) sample.remove(SensingUtils.MotionKeys.CALIBRATION);

                float   x, y, z,
                        xOffset, yOffset, zOffset;

                x = sample.remove(SensingUtils.MotionKeys.X).getAsFloat();
                y = sample.remove(SensingUtils.MotionKeys.Y).getAsFloat();
                z = sample.remove(SensingUtils.MotionKeys.Z).getAsFloat();

                xOffset = offsets.get(SensingUtils.MotionKeys.X).getAsFloat();
                yOffset = offsets.get(SensingUtils.MotionKeys.Y).getAsFloat();
                zOffset = offsets.get(SensingUtils.MotionKeys.Z).getAsFloat();

                sample.addProperty(SensingUtils.MotionKeys.X, x - xOffset);
                sample.addProperty(SensingUtils.MotionKeys.Y, y - yOffset);
                sample.addProperty(SensingUtils.MotionKeys.Z, z - zOffset);

                return false;
            }

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;

                for(JsonObject sample : ctx.getInput())
                    normalizeSample(sample);

            }
        }

        /**
         * Accelerometer-based readings are mapped according to the device's coordinate system, that
         * depends on the device's orientation. To assure the robustness of the this pipeline
         * there is a need to project the linear acceleration sensor's readings onto an absolute
         * coordinate system, more specifically the Earth's coordinate systems.
         * <br>
         * This projection process is based upon the one described by <a href="http://www.ami-lab.org/uploads/Publications/Journal/WP2/30_A%20Robust%20Dead-Reckoning%20Pedestrian%20Tracking%20System%20with%20Low%20Cost%20Sensors.pdf">[Jin:2011]</a>
         */
        public static class ProjectionStage implements Stage {

            private final Gson gson = new Gson();

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                for(JsonObject sample : input)
                    projectToAbsoluteCoordinates(sample);
            }

            private float[] extractAccelerationValues(JsonObject sample){
                float[] values = new float[4];
                values[0] = sample.get(SensingUtils.MotionKeys.X).getAsFloat();
                values[1] = sample.get(SensingUtils.MotionKeys.Y).getAsFloat();
                values[2] = sample.get(SensingUtils.MotionKeys.Z).getAsFloat();
                values[3] = 0;

                return values;
            }

            private void appendProjectedValues(JsonObject sample, float[] projectValues){
                sample.addProperty(SensingUtils.MotionKeys.PROJECTED_X, projectValues[0]);
                sample.addProperty(SensingUtils.MotionKeys.PROJECTED_Y, projectValues[1]);
                sample.addProperty(SensingUtils.MotionKeys.PROJECTED_Z, projectValues[2]);
            }

            private JsonObject projectToAbsoluteCoordinates(JsonObject sample) {

                try {
                    JsonObject rotationObj = sample.remove(SensingUtils.MotionKeys.ROTATION).getAsJsonObject();
                    float[] IR = new float[16];

                    IR = gson.fromJson(
                            rotationObj.get(SensingUtils.RotationVectorKeys.INV_ROTATION_MATRIX).getAsString(),
                            float[].class);

                    float[] result = new float[4];
                    float[] accEvent = extractAccelerationValues(sample);

                    Matrix.multiplyMV(result, 0, IR, 0, accEvent, 0);

                    appendProjectedValues(sample, result);

                }catch (IllegalStateException e){
                    return null;
                }

                return sample;
            }
        }

        /**
         * Once all processing has been performed over the original sensor readings, the results
         * are stored into the device's internal database storage.
         */
        public static class FinalizeStage implements Stage {

            private final ScoutStorageManager storage = ScoutStorageManager.getInstance();

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                ctx.setOutput(ctx.getInput());

                JsonObject[] input = ctx.getInput();

                if(input.length == 1 && input[0]!=null) try {
                    storage.store(String.valueOf(SensingUtils.Sensors.LINEAR_ACCELERATION), input[0]);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * This stage is responsible for computing the feature vector, where the feature vector
         * is comprised of almost all of the time domain-based features described by
         * <a href="http://web.ist.utl.pt/~diogo.ferreira/papers/figo10preprocessing.pdf">[Figo:2010]</a>
         * <br>
         * More specifically this stage will compute the following stages:<br>
         * <ul>
         *     <li>Mean</li>
         *     <li>Median</li>
         *     <li>Variance</li>
         *     <li>Standard Deviation</li>
         *     <li>Maximum</li>
         *     <li>Minimum</li>
         *     <li>Range</li>
         *     <li>Root Mean Squares</li>
         *     <li>Zero-Crossings</li>
         *     <li>Mean-Crossings</li>
         *     <li>Median-Crossings</li>
         *     <li>Range-Crossings</li>
         * </ul>
         * <br>
         * There is actually no need to compute all of these features, however since at this stage
         * it is still not clear which features improve the classification process, all possible
         * features will be extracted.
         */
        public class OverkillZFeatureExtractionStage implements Stage {

            private JsonObject constructFeatureVector(double[] values, JsonObject location){

                JsonObject featureVector = new JsonObject();

                int numSamples;
                double  mean,
                        median,
                        variance,
                        stdDev,
                        range, max, min,
                        rms;                //Root Mean Squares

                int zeroCrossings,
                    meanCrossings,
                    medianCrossings,
                    rangeCrossings;


                numSamples = values.length;

                mean        = StatisticalMetrics.calculateMean(values);
                variance    = StatisticalMetrics.calculateVariance(values);
                stdDev      = StatisticalMetrics.calculateStandardDeviation(values);

                median      = EnvelopeMetrics.calculateMedian(values);
                range       = EnvelopeMetrics.calculateRange(values);
                max         = EnvelopeMetrics.calculateMax(values);
                min         = EnvelopeMetrics.calculateMin(values);

                rms         = RMS.calculateRootMeanSquare(values);

                zeroCrossings   = TimeDomainMetrics.countZeroCrossings(values, 0);
                meanCrossings   = TimeDomainMetrics.countZeroCrossings(values, mean);
                medianCrossings = TimeDomainMetrics.countZeroCrossings(values, median);
                rangeCrossings  = TimeDomainMetrics.countZeroCrossings(values, range);

                //Base Properties
                featureVector.addProperty(SensingUtils.GeneralFields.SENSOR_TYPE, RoadConditionMonitoringPipeline.SENSOR_TYPE);
                featureVector.addProperty(SensingUtils.GeneralFields.TIMESTAMP, location.get(SensingUtils.GeneralFields.TIMESTAMP).getAsString());
                featureVector.addProperty(SensingUtils.GeneralFields.SCOUT_TIME, System.nanoTime());

                //Feature Properties
                featureVector.addProperty(SensingUtils.FeatureVectorKeys.NUM_SAMPLES, numSamples);
                featureVector.addProperty(SensingUtils.FeatureVectorKeys.MEAN, mean);
                featureVector.addProperty(SensingUtils.FeatureVectorKeys.MEDIAN, median);
                featureVector.addProperty(SensingUtils.FeatureVectorKeys.VARIANCE, variance);
                featureVector.addProperty(SensingUtils.FeatureVectorKeys.STDEV, stdDev);
                featureVector.addProperty(SensingUtils.FeatureVectorKeys.MAX, max);
                featureVector.addProperty(SensingUtils.FeatureVectorKeys.MIN, min);
                featureVector.addProperty(SensingUtils.FeatureVectorKeys.RANGE, range);
                featureVector.addProperty(SensingUtils.FeatureVectorKeys.RMS, rms);
                featureVector.addProperty(SensingUtils.FeatureVectorKeys.ZERO_CROSSING, zeroCrossings);
                featureVector.addProperty(SensingUtils.FeatureVectorKeys.MEAN_CROSSING, meanCrossings);
                featureVector.addProperty(SensingUtils.FeatureVectorKeys.MEDIAN_CROSSING, medianCrossings);
                featureVector.addProperty(SensingUtils.FeatureVectorKeys.RANGE_CROSSING, rangeCrossings);

                //Location Property
                featureVector.addProperty(SensingUtils.LocationKeys.SPEED, location.get(SensingUtils.LocationKeys.SPEED).getAsString());
                featureVector.add(SensingUtils.LocationKeys.LOCATION, location);

                return featureVector;
            }

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                int i=0;
                double[] zValues = new double[input.length];
                JsonObject featureVector=null, location = null;

                location = (JsonObject) input[0].get(SensingUtils.LocationKeys.LOCATION);

                for (JsonObject sample : input)
                    zValues[i++] = sample.get(SensingUtils.MotionKeys.PROJECTED_Z).getAsFloat();

                featureVector = constructFeatureVector(zValues, location);

                JsonObject[] output = new JsonObject[1];
                output[0] = featureVector;
                ctx.setInput(output);
            }
        }


        /**
         * Since at the point Scout it is still not able to classify the pavement type, this
         * stage will use user-provided classifications to tag the samples, which will then be used
         * to create a training data-set for the supervised-learning phase of the project.
         */
        public class TagForLearningStage implements Stage{

            @Override
            public void execute(PipelineContext pipelineContext) {
                JsonObject[] input = ((SensorPipelineContext)pipelineContext).getInput();

                if(input.length == 1 && input[0]!=null){
                    PavementType type = PavementType.getInstance();
                    input[0].addProperty("CLASS", type.getPavementType());
                }

            }
        }

        //Storage Stages - for graph construction and learning
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

                for(JsonObject sample : input)
                    testStorage.storeAccelerometerTestValue(testID, sample);

            }
        }

        public class StoreRawValuesStage extends StoreValuesForTestStage {
            public StoreRawValuesStage() {
                super("raw");
            }
        }

        public class StoreNormalizedValuesStage extends StoreValuesForTestStage {

            public StoreNormalizedValuesStage() {
                super("normalized");
            }
        }

        public class StoreProjectedValuesStage extends StoreValuesForTestStage {


            public StoreProjectedValuesStage() {
                super("projected");
            }
        }

        public class StoreFeatureVectorStage implements Stage {

            private LearningSupportStorage wekaStorage = LearningSupportStorage.getInstance();

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                for(JsonObject featureVector : input)
                    if(featureVector != null) wekaStorage.storeFeatureVector(featureVector);
            }
        }
    }
}
