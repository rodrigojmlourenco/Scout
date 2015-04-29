package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.location;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.location.LocationUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.FeatureExtractor;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.SensorProcessingPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.stages.MergeStage;

/**
 * @author rodrigo.jm.lourenco
 *         <p/>
 *         The PressureSensorPipeline is the class responsible for dealing with samples originated from
 *         a funf.PressureSensorProbe.
 *         <br>
 *         The main purpose behind this pipeline is to extract more realiable elevations through the captured
 *         atmospheric pressure.
 *         <br>
 *         This pipeline contemplates two stages:
 *         <ul>
 *         <li>
 *         <h3>MergeStage</h3>
 *         <p>
 *         This stage is responsible for merging closely related pressure samples, where two
 *         samples are considered closely related if they have both occured in a small time
 *         window frame. Closely related samples are merged by averaging their measured pressures.
 *         Although it is a known fact that the mean is very susceptible to outlier poisoning,
 *         unlike GPS altitude, measured pressure are very consistent.
 *         <br>
 *         Despite the consistency of measured pressure samples, it should be considere as future
 *         work the implementation of a AdmissionControlStage.
 *         </p>
 *         </li>
 *         <li>
 *         <h3>FeatureExtractionStage</h3>
 *         <p/>
 *         Feature extraction is this pipeline's final stage, that is responsible for deriving
 *         the altitude from the measured atmospheric pressure.
 * @version 1.0
 * @see <a href="http://developer.android.com/reference/android/hardware/SensorManager.html#getAltitude(float, float)">SensorManager.getAltitude(float p0, float p)</a>
 * </p>
 * </li>
 * </ul>
 * @see edu.mit.media.funf.probe.builtin.PressureSensorProbe
 */
public class PressureSensorPipeline implements SensorProcessingPipeline, FeatureExtractor {

    private static final String TAG = "[PRESSURE]";
    private static final String LOG_TAG = "PressurePipeline";

    private static final SensorPipeline PRESSURE_PIPELINE = new SensorPipeline();

    //Debugging
    private ScoutLogger logger = ScoutLogger.getInstance();

    private Queue<JsonObject> samplesQueue;
    private Queue<JsonObject> extractedFeaturesQueue;

    static {
        //PRESSURE_PIPELINE.addStage(new MergeStage(new PressureMergeStrategy()));
        PRESSURE_PIPELINE.addStage(new FeatureExtractionStage());
    }

    public PressureSensorPipeline() {
        this.samplesQueue = new LinkedList<>();
        this.extractedFeaturesQueue = new LinkedList<>();
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
    public JsonObject[] consumeExtractedFeatures() {

        JsonObject[] clone = new JsonObject[extractedFeaturesQueue.size()];
        extractedFeaturesQueue.toArray(clone);
        extractedFeaturesQueue.clear();

        return clone;
    }

    @Override
    public void run() {

        JsonObject[] input;

        //Merge the two queues, clear them, and pass the result as input
        synchronized (this) {

            if (!samplesQueue.isEmpty()) {
                input = new JsonObject[samplesQueue.size()];
                samplesQueue.toArray(input);
                samplesQueue.clear();
            } else
                return;


        }

        logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG + "executing pipeline for " + input.length + " motion samples.");

        //Execute the pipeline
        SensorPipelineContext context = new SensorPipelineContext();
        context.setInput(input);
        PRESSURE_PIPELINE.execute(context);

        //Update
        JsonObject[] extractedFeatures = context.getOutput();

        Collections.addAll(this.extractedFeaturesQueue, extractedFeatures);

    }

    /****************************************************************************************
     * STAGES: Stages to be used by the Pressure Pipeline                                   *
     ****************************************************************************************/
    /**
     * @author rodrigo.jm.lourenco
     *         <p/>
     *         Feature extraction is this pipeline's final stage, that is responsible for deriving
     *         the altitude from the measured atmospheric pressure.
     * @version 1.0
     * @see <a href="http://developer.android.com/reference/android/hardware/SensorManager.html#getAltitude(float, float)">SensorManager.getAltitude(float p0, float p)</a>
     */
    public static class FeatureExtractionStage implements Stage {

        private final String TAG = "[Altitude]";
        private ScoutLogger logger = ScoutLogger.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {

            JsonObject[] input = ((SensorPipelineContext) pipelineContext).getInput();
            Queue<JsonObject> features = new LinkedList<>();

            float altitude;
            float atmosphericPressure;
            for (JsonObject sample : input) {

                if (sample.has(SensingUtils.LocationKeys.PRESSURE)) {
                    atmosphericPressure = sample.get(SensingUtils.LocationKeys.PRESSURE).getAsFloat();
                    //altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, atmosphericPressure);
                    altitude = LocationUtils.getAltitude(LocationUtils.PRESSURE_STANDARD_ATMOSPHERE, atmosphericPressure);
                    sample.addProperty(SensingUtils.LocationKeys.BAROMETRIC_ALTITUDE, altitude);
                    features.add(sample);
                } else {
                    logger.log(ScoutLogger.ERR, PressureSensorPipeline.TAG, String.valueOf(sample));
                }

            }

            JsonObject[] extractedFeatures = new JsonObject[features.size()];
            features.toArray(extractedFeatures);

            ((SensorPipelineContext) pipelineContext).setOutput(extractedFeatures);
        }
    }

    /**
     * This stage is responsible for merging closely related pressure samples, where two
     * samples are considered closely related if they have both occured in a small time
     * window frame. Closely related samples are merged by averaging their measured pressures.
     * Although it is a known fact that the mean is very susceptible to outlier poisoning,
     * unlike GPS altitude, measured pressure are very consistent.
     * <br>
     * Despite the consistency of measured pressure samples, it should be considere as future
     * work the implementation of a AdmissionControlStage.
     */
    private static class PressureMergeStrategy implements MergeStage.MergeStrategy {

        @Override
        public JsonObject mergeSamples(Collection<JsonObject> samples) {
            JsonObject merged = new JsonObject();

            int i = 0, size = samples.size();
            BigDecimal[] timestamps = new BigDecimal[size];

            float averagePressure = 0;
            for (JsonObject sample : samples) {
                averagePressure += sample.get(SensingUtils.LocationKeys.PRESSURE).getAsFloat();
                timestamps[i++] = new BigDecimal(sample.get(SensingUtils.LocationKeys.TIMESTAMP).getAsString());
            }

            //Time values
            Arrays.sort(timestamps);
            BigDecimal fistTimestamp = timestamps[0],
                    elapsedTime = timestamps[size - 1].subtract(fistTimestamp);

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
    }
}
