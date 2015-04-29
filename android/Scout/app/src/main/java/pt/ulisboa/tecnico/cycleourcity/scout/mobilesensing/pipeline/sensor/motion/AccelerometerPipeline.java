package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.motion;


import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import edu.mit.media.funf.json.IJsonObject;
import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain.StatisticalMetrics;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.SensorProcessingPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager;

/**
 * @version 1.1 New model
 * @author rodrigo.jm.lourenco
 *
 *
 */
public class AccelerometerPipeline implements SensorProcessingPipeline {

    private static final String TAG = "[MOTION]";
    private static final String LOG_TAG = "AccelerometerPipeline";
    public final static String SENSOR_TYPE = "Accelerometer";
    public final static String SENSOR_TYPE_GRAVITY = "Gravity";


    private static final SensorPipeline ACCELEROMETER_PIPELINE = new SensorPipeline();

    static{
        //ACCELEROMETER_PIPELINE.addStage(new AdmissionControlStage());
        //ACCELEROMETER_PIPELINE.addStage(new TrimStage());
        ACCELEROMETER_PIPELINE.addStage(new FeatureExtractionStage());
        ACCELEROMETER_PIPELINE.addFinalStage(new PostExecuteStage());
    }

    //Queues for further processing
    private Queue<JsonObject> gravitySamples;
    private Queue<JsonObject> accelerometerSamples;

    //Logging
    private ScoutLogger logger = ScoutLogger.getInstance();

    public AccelerometerPipeline(){

        //Queues initialization
        gravitySamples = new LinkedList<>();
        accelerometerSamples = new LinkedList<>();

    }

    @Override
    public void pushSample(JsonObject sample) {

        int sensorType = sample.get(SensingUtils.SENSOR_TYPE).getAsInt();

        switch (sensorType){
            case SensingUtils.ACCELEROMETER:
                accelerometerSamples.add(sample);
                break;
            case SensingUtils.GRAVITY:
                gravitySamples.add(sample);
                break;
        }
    }

    @Override
    public void pushSampleCollection(Collection<JsonObject> sampleCollection) {
        for(JsonObject sample : sampleCollection){
            pushSample(sample);
        }
    }

    @Override
    public void run() {

        JsonObject[] input;

        //Merge the two queues, clear them, and pass the result as input
        synchronized (this) {
            input = new JsonObject[accelerometerSamples.size() + gravitySamples.size()];
            Queue<JsonObject> merged = new LinkedList<>();
            merged.addAll(accelerometerSamples);
            merged.addAll(gravitySamples);
            merged.toArray(input);

            gravitySamples.clear();
            accelerometerSamples.clear();
        }

        logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG+"executing pipeline for "+input.length+" motion samples.");

        SensorPipelineContext context = new SensorPipelineContext();
        context.setInput(input);
        ACCELEROMETER_PIPELINE.execute(context);
    }

    /****************************************************************************************
     * STAGES: Stages to be used by the Accelerometer Pipeline                              *
     ****************************************************************************************/

    public static class AdmissionControlStage implements Stage {

        @Override
        public void execute(PipelineContext pipelineContext) {
            //TODO: ainda não é claro como posso discartar amostras, talvez apenas recorrendo ao ScoutState
        }
    }

    /**
     * @version 1.0
     * @author rodrigo.jm.lourenco
     *
     * Unlike the information gathered by the LocationProbe, the accelerometer and gravity probes
     * generate simple information.
     * <br>
     * This stage will only replace the numeric representation of the sensor type by a string.
     */
    public static class TrimStage implements Stage {

        @Override
        public void execute(PipelineContext pipelineContext) {

            JsonObject[] input =((SensorPipelineContext)pipelineContext).getInput();

            //TODO: corrigir de forma a contemplar gravidade;
            for(JsonObject sample : input){
                sample.remove(SensingUtils.SENSOR_TYPE);
                sample.remove(SensingUtils.MotionKeys.ACCURACY);
                sample.addProperty(SensingUtils.SENSOR_TYPE, SENSOR_TYPE);
            }

            ((SensorPipelineContext)pipelineContext).setInput(input); //Next Stage
        }
    }

    /**
     * @version 1.0 Time-Domain Statistical Features Only
     * @author rodrigo.jm.lourenco
     */
    public static class FeatureExtractionStage implements Stage {

        private final static int X = 0;
        private final static int Y = 1;
        private final static int Z = 2;

        public final static String MEAN = "Mean";
        public final static String VARIANCE = "Variance";
        public final static String STANDARD_DEVIATION = "Standard Deviation";
        public final static String MAX_DEVIATION = "Max Deviation";
        public final static String ABSOLUTE_CENTRAL_MOMENT = "Abs Central Moment";
        public final static String PSD_ACROSS_FREQUENCY_BANDS = "PSD Accoss Freq Bands";

        //Logging
        private ScoutLogger logger = ScoutLogger.getInstance();

        private JsonObject calculateMean(MotionSignals motions){
            JsonObject data = new JsonObject();

            double xMean = StatisticalMetrics.calculateMean(motions.getXSignals());
            double yMean = StatisticalMetrics.calculateMean(motions.getYSignals());
            double zMean = StatisticalMetrics.calculateMean(motions.getZSignals());

            data.addProperty(SensingUtils.MotionKeys.X, xMean);
            data.addProperty(SensingUtils.MotionKeys.Y, yMean);
            data.addProperty(SensingUtils.MotionKeys.Z, zMean);

            return data;
        }

        private JsonObject calculateStandardDeviation(MotionSignals motions){
            JsonObject data = new JsonObject();

            double xStdDev = StatisticalMetrics.calculateStandardDeviation(motions.getXSignals());
            double yStdDev = StatisticalMetrics.calculateStandardDeviation(motions.getYSignals());
            double zStdDev = StatisticalMetrics.calculateStandardDeviation(motions.getZSignals());

            data.addProperty(SensingUtils.MotionKeys.X, xStdDev);
            data.addProperty(SensingUtils.MotionKeys.Y, yStdDev);
            data.addProperty(SensingUtils.MotionKeys.Z, zStdDev);

            return data;
        }

        private JsonObject calculateVariance(MotionSignals motions){
            JsonObject data = new JsonObject();

            double xVar = StatisticalMetrics.calculateVariance(motions.getXSignals());
            double yVar = StatisticalMetrics.calculateVariance(motions.getYSignals());
            double zVar = StatisticalMetrics.calculateVariance(motions.getZSignals());

            data.addProperty(SensingUtils.MotionKeys.X, xVar);
            data.addProperty(SensingUtils.MotionKeys.Y, yVar);
            data.addProperty(SensingUtils.MotionKeys.Z, zVar);

            return data;
        }

        private void setExtractedFeatured(JsonObject data, MotionSignals motionSignals){

            data.add(MEAN, calculateMean(motionSignals));
            data.add(VARIANCE, calculateVariance(motionSignals));
            data.add(STANDARD_DEVIATION, calculateStandardDeviation(motionSignals));
        }

        private JsonObject getExtractedFeaturesAsJson(String sensor, MotionSignals signals, TimeKeeper time){

            JsonObject extractedFeatures = new JsonObject();

            extractedFeatures.addProperty(SensingUtils.SENSOR_TYPE, sensor);
            extractedFeatures.addProperty(SensingUtils.MotionKeys.TIMESTAMP, time.getAvgTime());
            extractedFeatures.addProperty(SensingUtils.MotionKeys.ELAPSED_TIME, time.getElapsedTime());
            extractedFeatures.addProperty(SensingUtils.MotionKeys.SAMPLES, signals.getTotalSamples());
            setExtractedFeatured(extractedFeatures, signals);

            return extractedFeatures;
        }

        @Override
        public void execute(PipelineContext pipelineContext) {

            JsonObject[] input =((SensorPipelineContext)pipelineContext).getInput();
            MotionSignals accMotionSignals = new MotionSignals(),
                         gravityMotionSignals = new MotionSignals();

            double x, y, z;

            //Timestamping
            double timestamp = 0;
            TimeKeeper accTimeKeeper = new TimeKeeper();
            TimeKeeper gravTimeKeeper = new TimeKeeper();

            //Samples Counters
            int accSamples = 0;
            int gravSamples = 0;

            //PHASE-1 Separate the different samples, according to it's provider
            for(JsonObject sample : input){
                int sensorType = sample.get(SensingUtils.SENSOR_TYPE).getAsInt();

                //Extract the motion Signal
                x = sample.get(SensingUtils.MotionKeys.X).getAsDouble();
                y = sample.get(SensingUtils.MotionKeys.Y).getAsDouble();
                z = sample.get(SensingUtils.MotionKeys.Z).getAsDouble();

                timestamp = sample.get(SensingUtils.MotionKeys.TIMESTAMP).getAsDouble();

                switch (sensorType){
                    case SensingUtils.ACCELEROMETER:
                        accSamples++;
                        accMotionSignals.addSample(x,y,z);
                        accTimeKeeper.addTimestamp(timestamp);
                        break;
                    case SensingUtils.GRAVITY:
                        gravSamples++;
                        gravityMotionSignals.addSample(x,y,z);
                        gravTimeKeeper.addTimestamp(timestamp);
                        break;
                    default:
                        logger.log(ScoutLogger.WARN, LOG_TAG, TAG+"Unknown sensor type '"+sensorType+"'.");
                }
            }

            //PHASE-2: Extract Features from the captured signals
            //TODO: Linear Acceleration
            JsonObject[] output;
            if(gravityMotionSignals.getTotalSamples() >= 0){
                output = new JsonObject[2];
                output[1] = getExtractedFeaturesAsJson(SENSOR_TYPE_GRAVITY, gravityMotionSignals, gravTimeKeeper);
            }else
                output = new JsonObject[1];

            output[0] = getExtractedFeaturesAsJson(SENSOR_TYPE, accMotionSignals, accTimeKeeper);

            ((SensorPipelineContext)pipelineContext).setInput(output);
        }

        private class TimeKeeper {

            private double minTime=0, maxTime=0, avgTime=0;
            private int timeSamples = 0;
            private boolean first = true;

            public void addTimestamp(double timestamp){

                timeSamples++;
                avgTime += timestamp;

                if(first) {
                    first = false;
                    minTime = maxTime = timestamp;
                }else{
                    minTime = (timestamp<minTime) ? timestamp : minTime;
                    maxTime = (timestamp>maxTime) ? timestamp : maxTime;
                }
            }

            public double getAvgTime(){ return avgTime/timeSamples; }
            public double getElapsedTime(){ return maxTime-minTime; }
        }

        private class MotionSignals {

            private List<Double> xSignals = new ArrayList<>(),
                    ySignals = new ArrayList<>(),
                    zSignals = new ArrayList<>();


            public void addSample(double x, double y, double z){
                xSignals.add(x);
                ySignals.add(y);
                zSignals.add(z);
            }

            private double[] doubleListToArray(List<Double> list){

                int i=0;
                double[] out = new double[list.size()];

                for(Double l : list) {
                    out[i] = l;
                    i++;
                }

                return out;
            }

            public double[] getXSignals() {
                return doubleListToArray(xSignals);
            }
            public double[] getYSignals() {
                return doubleListToArray(ySignals);
            }
            public double[] getZSignals() {
                return doubleListToArray(zSignals);
            }
            public int getTotalSamples()  { return xSignals.size(); }
        }
    }

    /**
     * @version 1.0
     * This stage operates as a callback function, it extracts the output from the PipelineContext,
     * which is basically the extracted features, and stores it both in an extracted feature queue
     * and on the application's storage manager.
     *
     * @see pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext
     * @see pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager
     *
     * TODO: esta stage deve ser igual para todos os pipelines pelo que pode ser externa
     */
    public static class PostExecuteStage implements Stage {

        private ScoutLogger logger = ScoutLogger.getInstance();
        private ScoutStorageManager storage = ScoutStorageManager.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {
            logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG+"pre-processing terminated.");

            int storedFeatures = 0;
            JsonObject[] output = ((SensorPipelineContext)pipelineContext).getInput();

            for(JsonObject feature : output){

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

    /**
     *
     */
    public static class UpdateScoutStateStage implements Stage {

        @Override
        public void execute(PipelineContext pipelineContext) {

        }
    }
}
