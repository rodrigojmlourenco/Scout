package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.location;

import android.hardware.SensorManager;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import edu.mit.media.funf.json.IJsonObject;
import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeLineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor.FeatureExtractor;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor.ISensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.parser.SensingUtils;

/**
 * Created by rodrigo.jm.lourenco on 24/04/2015.
 */
public class PressureSensorPipeline implements ISensorPipeline, FeatureExtractor {

    private static final String TAG = "[PRESSURE]";
    private static final String LOG_TAG = "PressurePipeline";

    private static final SensorPipeline PRESSURE_PIPELINE = new SensorPipeline();

    //Debugging
    private ScoutLogger logger = ScoutLogger.getInstance();

    private Queue<JsonObject> samplesQueue;
    private Queue<JsonObject> extractedFeaturesQueue;

    static{
        PRESSURE_PIPELINE.addStage(new MergeStage());
        PRESSURE_PIPELINE.addFinalStage(new FeatureExtractionStage());
    }

    public PressureSensorPipeline(){
        this.samplesQueue = new LinkedList<>();
        this.extractedFeaturesQueue = new LinkedList<>();
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
    public JsonObject[] consumeExtractedFeatures(){

        JsonObject[] clone = new JsonObject[extractedFeaturesQueue.size()];
        extractedFeaturesQueue.toArray(clone);
        extractedFeaturesQueue.clear();

        return  clone;
    }

    @Override
    public void run() {

        JsonObject[] input;

        //Merge the two queues, clear them, and pass the result as input
        synchronized (this) {

            if(!samplesQueue.isEmpty()){
                input = new JsonObject[samplesQueue.size()];
                samplesQueue.toArray(input);
                samplesQueue.clear();
            }else
                return;



        }

        logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG+"executing pipeline for "+input.length+" motion samples.");

        //Execute the pipeline
        SensorPipeLineContext context = new SensorPipeLineContext();
        context.setInput(input);
        PRESSURE_PIPELINE.execute(context);

        //Update
        JsonObject[] extractedFeatures = context.getOutput();

        for(JsonObject feature : extractedFeatures)
            this.extractedFeaturesQueue.add(feature);

    }

    /****************************************************************************************
     * STAGES: Stages to be used by the Pressure Pipeline                                   *
     ****************************************************************************************/

    public static class MergeStage implements Stage {

        private ScoutLogger logger = ScoutLogger.getInstance();

        public final static int MAX_TIME_DISTANCE = 1000;//ms

        private boolean closelyRelated(JsonObject sample1, JsonObject sample2){

            //Avoid NullPointerException
            if(sample1 == null || sample2 == null) return false;

            long t1, t2, elapsed;

            t1 = sample1.get(SensingUtils.TIMESTAMP).getAsLong();
            t2 = sample2.get(SensingUtils.TIMESTAMP).getAsLong();;

            elapsed = Math.abs(t2-t1);

            return elapsed < MAX_TIME_DISTANCE;
        }

        private JsonObject mergeSamples(List<JsonObject> samples){

            JsonObject merged = new JsonObject();

            int i=0, size = samples.size();
            long[] timestamps = new long[size];

            float averagePressure = 0;
            for (JsonObject sample : samples){
                averagePressure+=sample.get(SensingUtils.LocationKeys.PRESSURE).getAsFloat();
                timestamps[i++]=sample.get(SensingUtils.LocationKeys.TIMESTAMP).getAsLong();
            }

            //Time values
            Arrays.sort(timestamps);
            long    fistTimestamp = timestamps[0],
                    elapsedTime = timestamps[size-1]-fistTimestamp;

            //Pressure Values
            averagePressure /= size;

            //Reconstruct Sample as One
            merged.addProperty(SensingUtils.SENSOR_TYPE, SensingUtils.PRESSURE);
            merged.addProperty(SensingUtils.TIMESTAMP, fistTimestamp);
            merged.addProperty(SensingUtils.LocationKeys.ELAPSED_TIME, elapsedTime);
            merged.addProperty(SensingUtils.ACCURACY, 3);
            merged.addProperty(SensingUtils.LocationKeys.PRESSURE, averagePressure);
            merged.addProperty(SensingUtils.LocationKeys.SAMPLES, size);

            return merged;
        }

        @Override
        public void execute(PipelineContext pipelineContext) {

            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();

            JsonObject patientZero = input[0];
            ArrayList<JsonObject> samples2Merge = new ArrayList<>();
            Queue<JsonObject> mergedSamples = new LinkedList<>();

            int accuracy;
            for(JsonObject sample : input){

                //Disregard sample if it has low accuracy
                accuracy = sample.get(SensingUtils.ACCURACY).getAsInt();
                if(accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {

                    if (closelyRelated(patientZero, sample))
                        samples2Merge.add(sample);
                    else {
                        patientZero = sample;
                        mergedSamples.add(mergeSamples(samples2Merge));
                        samples2Merge.clear();
                    }
                }
            }

            if(!samples2Merge.isEmpty())
                mergedSamples.add(mergeSamples(samples2Merge));

            JsonObject[] mergedInput = new JsonObject[mergedSamples.size()];
            mergedSamples.toArray(mergedInput);
            ((SensorPipeLineContext)pipelineContext).setInput(mergedInput);
        }
    }

    public static class FeatureExtractionStage implements Stage {

        private final String TAG = "[Altitude]";
        private ScoutLogger logger = ScoutLogger.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {

            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();
            Queue<JsonObject> features = new LinkedList<>();

            float altitude;
            float atmosphericPressure;
            for(JsonObject sample : input){

                if(sample.has(SensingUtils.LocationKeys.PRESSURE)){
                    atmosphericPressure = sample.get(SensingUtils.LocationKeys.PRESSURE).getAsFloat();
                    altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, atmosphericPressure);
                    sample.addProperty(SensingUtils.LocationKeys.BAROMETRIC_ALTITUDE, altitude);
                    features.add(sample);
                }else{
                    logger.log(ScoutLogger.ERR, PressureSensorPipeline.TAG, String.valueOf(sample));
                }

            }

            JsonObject[] extractedFeatures = new JsonObject[features.size()];
            features.toArray(extractedFeatures);

            ((SensorPipeLineContext)pipelineContext).setOutput(extractedFeatures);
        }
    }
}
