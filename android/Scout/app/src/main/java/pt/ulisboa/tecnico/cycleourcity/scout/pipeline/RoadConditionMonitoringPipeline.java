package pt.ulisboa.tecnico.cycleourcity.scout.pipeline;


import android.opengl.Matrix;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.sql.SQLException;
import java.util.Arrays;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.PipelineConfiguration;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.ConfigurationCaretaker;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.SensorProcessingPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.stages.CommonStages;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager;

/**
 * Created by rodrigo.jm.lourenco on 30/06/2015.
 */
public class RoadConditionMonitoringPipeline extends SensorProcessingPipeline {

    public RoadConditionMonitoringPipeline(ConfigurationCaretaker caretaker) {
        super(SensingUtils.LINEAR_ACCELERATION, caretaker);
    }

    public static PipelineConfiguration generateRoadConditionMonitoringPipelineConfiguration(){
        ScoutStorageManager storage = ScoutStorageManager.getInstance();

        PipelineConfiguration configuration = new PipelineConfiguration();
        //configuration.addStage(new RoadConditionMonitoringStages.FramingStage());
        configuration.addStage(new RoadConditionMonitoringStages.ProjectionStage());
        configuration.addStage(new RoadConditionMonitoringStages.BruteForceMerge());

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

        public static class FramingStage implements Stage {

            public final static int NUM_SAMPLES_PER_FRAME = 128;


            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                int totalSamples = input.length,
                        totalFrames = input.length/NUM_SAMPLES_PER_FRAME;

                JsonObject[] auxFrame = new JsonObject[NUM_SAMPLES_PER_FRAME];
                JsonObject[] framedInput = new JsonObject[totalFrames];

                Gson gson = new Gson();
                int i, j, currSample=0;
                for(i=0; i < totalFrames; i++) {

                    Arrays.fill(auxFrame, null);
                    for (j = 0; j < NUM_SAMPLES_PER_FRAME; j++, currSample++)
                        auxFrame[j] = input[currSample];

                    JsonObject frame = new JsonObject();
                    frame.addProperty("frame", i);
                    frame.addProperty("values", gson.toJson(auxFrame));
                    framedInput[i] = frame;

                }

                Log.d(LOG_TAG, "=== BEGIN ===");
                for(JsonObject obj : framedInput)
                    Log.d(LOG_TAG, String.valueOf(obj));
            }
        }

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

        public static class BruteForceMerge implements Stage{

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                int projectedSamples = 0;
                float x=0, y=0, z=0, px=0, py=0, pz=0;

                for(JsonObject sample : input){
                    x += sample.get(SensingUtils.MotionKeys.X).getAsFloat();
                    y += sample.get(SensingUtils.MotionKeys.Y).getAsFloat();
                    z += sample.get(SensingUtils.MotionKeys.Z).getAsFloat();

                    if(sample.has(SensingUtils.MotionKeys.PROJECTED_X)) {
                        px += sample.get(SensingUtils.MotionKeys.PROJECTED_X).getAsFloat();
                        py += sample.get(SensingUtils.MotionKeys.PROJECTED_Y).getAsFloat();
                        pz += sample.get(SensingUtils.MotionKeys.PROJECTED_Z).getAsFloat();
                        projectedSamples++;
                    }
                }

                x /= input.length;
                y /= input.length;
                z /= input.length;

                px /= projectedSamples;
                py /= projectedSamples;
                pz /= projectedSamples;

                JsonObject mergedSample = new JsonObject();
                mergedSample.addProperty(SensingUtils.TIMESTAMP, SensingUtils.LINEAR_ACCELERATION);
                mergedSample.addProperty(SensingUtils.SCOUT_TIME, System.nanoTime());

                mergedSample.addProperty(SensingUtils.MotionKeys.X, x);
                mergedSample.addProperty(SensingUtils.MotionKeys.Y, y);
                mergedSample.addProperty(SensingUtils.MotionKeys.Z, z);
                mergedSample.addProperty("NativeSamplesTotal", input.length);

                mergedSample.addProperty(SensingUtils.MotionKeys.PROJECTED_X, px);
                mergedSample.addProperty(SensingUtils.MotionKeys.PROJECTED_Y, py);
                mergedSample.addProperty(SensingUtils.MotionKeys.PROJECTED_Z, pz);
                mergedSample.addProperty("ProjectedSamplesTotal", projectedSamples);

                Log.d(LOG_TAG, String.valueOf(mergedSample));

                JsonObject[] output = new JsonObject[1];
                output[0] = mergedSample;
                ctx.setInput(output);
            }
        }

        public static class FinalizeStage implements Stage {

            private final ScoutStorageManager storage = ScoutStorageManager.getInstance();

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                ctx.setOutput(ctx.getInput());

                JsonObject[] input = ctx.getInput();

                if(input.length == 1) try {
                    storage.store(""+SensingUtils.LINEAR_ACCELERATION, input[0]);
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                float[] nativeValues = new float[3],
                        projectedValues = new float[3];

                nativeValues[0] = input[0].get(SensingUtils.MotionKeys.X).getAsFloat();
                nativeValues[1] = input[0].get(SensingUtils.MotionKeys.Y).getAsFloat();
                nativeValues[2] = input[0].get(SensingUtils.MotionKeys.Z).getAsFloat();
                ScoutStorageManager.NATIVE.storeValues(nativeValues);

                projectedValues[0] = input[0].get(SensingUtils.MotionKeys.PROJECTED_X).getAsFloat();
                projectedValues[1] = input[0].get(SensingUtils.MotionKeys.PROJECTED_Y).getAsFloat();
                projectedValues[2] = input[0].get(SensingUtils.MotionKeys.PROJECTED_Z).getAsFloat();
                ScoutStorageManager.PROJECTED.storeValues(projectedValues);

            }
        }
    }
}
