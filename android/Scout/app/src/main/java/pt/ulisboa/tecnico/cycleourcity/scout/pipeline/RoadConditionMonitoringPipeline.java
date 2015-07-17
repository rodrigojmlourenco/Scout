package pt.ulisboa.tecnico.cycleourcity.scout.pipeline;

import android.opengl.Matrix;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.sql.SQLException;

import pt.ulisboa.tecnico.cycleourcity.scout.learning.PavementType;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain.EnvelopeMetrics;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain.RMS;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain.StatisticalMetrics;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain.TimeDomainMetrics;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.PipelineConfiguration;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.ConfigurationCaretaker;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.SensorProcessingPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.stages.CommonStages;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.GraphValuesStorage;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.LearningSupportStorage;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager;

/**
 * Created by rodrigo.jm.lourenco on 30/06/2015.
 */
public class RoadConditionMonitoringPipeline extends SensorProcessingPipeline {

    public final static int SENSOR_TYPE = SensingUtils.LINEAR_ACCELERATION;

    public RoadConditionMonitoringPipeline(ConfigurationCaretaker caretaker) {
        super(SENSOR_TYPE, caretaker);
    }

    public static PipelineConfiguration generateRoadConditionMonitoringPipelineConfiguration(){
        ScoutStorageManager storage = ScoutStorageManager.getInstance();

        PipelineConfiguration configuration = new PipelineConfiguration();

        configuration.addStage(new RoadConditionMonitoringStages.StoreRawValuesStage());            //TESTING
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

                if(!sample.has("rotation") || sample.get("rotation")==null)
                    return null;

                try {
                    JsonObject rotationObj = sample.remove("rotation").getAsJsonObject();
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


        public static class FinalizeStage implements Stage {

            private final ScoutStorageManager storage = ScoutStorageManager.getInstance();

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                ctx.setOutput(ctx.getInput());

                JsonObject[] input = ctx.getInput();

                if(input.length == 1 && input[0]!=null) try {
                    storage.store(""+SensingUtils.LINEAR_ACCELERATION, input[0]);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

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
                featureVector.addProperty(SensingUtils.SENSOR_TYPE, RoadConditionMonitoringPipeline.SENSOR_TYPE);
                featureVector.addProperty(SensingUtils.TIMESTAMP, location.get(SensingUtils.TIMESTAMP).getAsString());
                featureVector.addProperty(SensingUtils.SCOUT_TIME, System.nanoTime());

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

                try {
                    if (input[0] != null && input[0].has(SensingUtils.LocationKeys.LOCATION))
                        location = (JsonObject) input[0].get(SensingUtils.LocationKeys.LOCATION);
                }catch (ClassCastException e){
                    Log.e(LOG_TAG, String.valueOf(input[0]));
                    e.printStackTrace();
                }

                try {

                    for (JsonObject sample : input)
                        zValues[i++] = sample.get(SensingUtils.MotionKeys.PROJECTED_Z).getAsFloat();

                    featureVector = constructFeatureVector(zValues, location);

                }catch (NullPointerException e){
                    e.printStackTrace();
                }

                JsonObject[] output = new JsonObject[1];
                output[0] = featureVector;
                ctx.setInput(output);
            }
        }

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
            private GraphValuesStorage testStorage = GraphValuesStorage.getInstance();

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

            public final static String TEST_ID = "raw";

            public StoreRawValuesStage() {
                super(TEST_ID);
            }
        }

        public class StoreNormalizedValuesStage extends StoreValuesForTestStage {

            public final static String TEST_ID = "normalized";

            public StoreNormalizedValuesStage() {
                super(TEST_ID);
            }
        }

        public class StoreProjectedValuesStage extends StoreValuesForTestStage {

            public final static String TEST_ID = "projected";

            public StoreProjectedValuesStage() {
                super(TEST_ID);
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
