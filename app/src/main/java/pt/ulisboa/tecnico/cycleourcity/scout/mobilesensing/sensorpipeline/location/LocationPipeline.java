package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.location;

import android.util.Log;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import edu.mit.media.funf.json.IJsonObject;
import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeLineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor.FeatureExtractor;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor.ISensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.data.Location;
import pt.ulisboa.tecnico.cycleourcity.scout.parser.SensingUtils;

/**
 * Created by rodrigo.jm.lourenco on 24/04/2015.
 */
public class LocationPipeline implements ISensorPipeline {

    public final static String TAG = "[Location]";
    public final String LOG_TAG = this.getClass().getSimpleName();

    private PressureSensorPipeline pressureSensorPipeline;
    private LocationSensorPipeline locationSensorPipeline;

    private Queue<JsonObject> samplesQueue;

    private static SensorPipeline LOCATION_PIPELINE = new SensorPipeline();
    static {
        LOCATION_PIPELINE.addStage(new DispatchSensorSamplesStage());
        LOCATION_PIPELINE.addStage(new MergeStage());
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

            JsonObject[] dispathOutput = new JsonObject[merger.size()];
            merger.toArray(dispathOutput);

            ((SensorPipeLineContext)pipelineContext).setInput(dispathOutput);
            logger.log(ScoutLogger.DEBUG, LocationPipeline.TAG, "Finished all tasks");

        }
    }

    public static class MergeStage implements Stage {

        public final int MAX_TIME_DISTANCE = 1000;//ms

        private JsonObject mergeSamples(Queue<JsonObject> samples){

            JsonObject merged = new JsonObject();

            return merged;
        }

        private boolean closelyRelated(JsonObject sample1, JsonObject sample2){

            //USAR ISTO PARA CONVERTER
            /*timestampMillis = */

            //Avoid NullPointerException
            if(sample1 == null || sample2 == null) return false;

            if(sample1.get(SensingUtils.SENSOR_TYPE)!=sample2.get(SensingUtils.SENSOR_TYPE))
                Log.d("SOME", "SOME");

            long t1, t2, elapsed;

            t1 = SensingUtils.LocationSampleAccessor.getTimestamp(sample1);
            t2 = SensingUtils.LocationSampleAccessor.getTimestamp(sample2);

            if(sample1.get(SensingUtils.SENSOR_TYPE).getAsInt()==SensingUtils.PRESSURE)
                t1 = (new Date()).getTime() + (t1 - System.nanoTime()) / 1000000L;

            if(sample2.get(SensingUtils.SENSOR_TYPE).getAsInt()==SensingUtils.PRESSURE)
                t2 = (new Date()).getTime() + (t2 - System.nanoTime()) / 1000000L;

            elapsed = Math.abs(t2-t1);

            return elapsed < MAX_TIME_DISTANCE;
        }

        private ScoutLogger logger = ScoutLogger.getInstance();
        @Override
        public void execute(PipelineContext pipelineContext) {
            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();

            if(input.length <= 0) return;

            JsonObject patientZero = input[0];
            Queue<JsonObject> samples2Merge = new LinkedList<>(),
                              mergedSamples = new LinkedList<>();

            for(JsonObject sample : input){
                if(closelyRelated(patientZero, sample))
                    samples2Merge.add(sample);
                else{
                    patientZero = sample;
                    mergedSamples.add(mergeSamples(samples2Merge));
                    samples2Merge.clear();
                }
            }



        }
    }
}
