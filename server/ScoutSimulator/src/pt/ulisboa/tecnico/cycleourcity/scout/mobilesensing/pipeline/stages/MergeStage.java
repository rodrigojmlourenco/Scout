package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.stages;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;

/**
 * @version 1.0
 * @author rodrigo.jm.lourenco
 *
 * The frequency rate of different sensor may lead to hundreds of samples being generated each second,
 * this Stage deals with this by merging samples, which are closely related.
 * <br>
 * This class behaviour is defined by two strategies:<br>
 * <ul>
 *      <li>CheckForRelationStrategy</li>
 *      <li>MergeStrategy</li>
 * </ul>
 * <br>
 * The first stategy defines the operation that checks if two samples are closely related. The second
 * defines the strategy to be employed in order to merge all closely related samples.
 *
 * @see pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.stages.MergeStage.MergeStrategy
 * @see pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.stages.MergeStage.CheckForRelationStrategy
 */
public class MergeStage implements Stage {

    public final String LOG_TAG = this.getClass().getSimpleName();

    private final MergeStrategy mergingStrategy;
    private final CheckForRelationStrategy relationCheckStrategy;

    private ScoutLogger logger = ScoutLogger.getInstance();


    public MergeStage(MergeStrategy strategy){
        this.mergingStrategy = strategy;
        this.relationCheckStrategy = new DefaultCheckForRelation();
    }

    public MergeStage(CheckForRelationStrategy relationCheckStrategy, MergeStrategy mergingStrategy){
        this.mergingStrategy = mergingStrategy;
        this.relationCheckStrategy = relationCheckStrategy;
    }


    @Override
    public void execute(PipelineContext pipelineContext) {

        JsonObject[] input = ((SensorPipelineContext)pipelineContext).getInput();

        //Avoid ArrayIndexOutOfBounds
        if (input.length<=0) return;

        JsonObject patientZero = input[0];
        Queue<JsonObject>   samples2Merge = new LinkedList<>(),
                            mergedSamples = new LinkedList<>();


        for(JsonObject sample : input){

            if (relationCheckStrategy.areCloselyRelated(patientZero, sample))
                samples2Merge.add(sample);
            else {
                patientZero = sample;
                mergedSamples.add(mergingStrategy.mergeSamples(samples2Merge));
                samples2Merge.clear();
            }
        }

        if(!samples2Merge.isEmpty())
            mergedSamples.add(mergingStrategy.mergeSamples(samples2Merge));

        //Debugging
        String sensorType = SensingUtils.getSensorTypeAsString(patientZero.get(SensingUtils.SENSOR_TYPE).getAsInt());
        logger.log(ScoutLogger.INFO, LOG_TAG, "Merged "+input.length+" "+sensorType+" samples into "+mergedSamples.size());

        JsonObject[] mergedInput = new JsonObject[mergedSamples.size()];
        mergedSamples.toArray(mergedInput);
        ((SensorPipelineContext)pipelineContext).setInput(mergedInput);
    }

    /**
     * @version 1.0
     * @author rodrigo.jm.lourenco
     * Defines if two samples are closely related.
     */
    public static interface CheckForRelationStrategy{
        /**
         * Given two samples, this method checks if these samples are closely related.
         * @param sample1 JsonObject sample one.
         * @param sample2 JsonObject sample two.
         * @return True if the samples are closely related, false otherwise.
         */
        public boolean areCloselyRelated(JsonObject sample1, JsonObject sample2);
    }

    /**
     * @version 1.0
     * @author rodrigo.jm.lourenco
     * Defines how a collection of samples should be merged.
     */
    public static interface MergeStrategy {
        /**
         * Given a collection of JsonObject samples, merges all the samples into one
         * @param samples Samples to merge
         * @return Merged sample
         */
        public JsonObject mergeSamples(Collection<JsonObject> samples);
    }

    /**
     * DefaultCheckForRelation is a concrete implementation of the CheckForRelationStrategy,
     * that defines that two samples are closely related if they have both occurred in a small
     * predefined window frame, where the time of each samples is given by it's timestamp field.
     * <br>
     * <emph>Note:</emph> It is important to note that the timestamp value is measured in nanoseconds.
     */
    public static class DefaultCheckForRelation implements CheckForRelationStrategy {


        //Time Utils
        public final static BigDecimal MILLIS_2_NANOS= new BigDecimal("1000000");
        public final static BigDecimal SECOND_2_NANOS= new BigDecimal("1000000000");
        public final static float NANO_2_MILLIS = 1/1000000;


        //Pre-defined Time Window
        public final static BigDecimal MAX_TIME_FRAME = SECOND_2_NANOS; //1s

        @Override
        public boolean areCloselyRelated(JsonObject sample1, JsonObject sample2) {

            //Avoid NullPointerException
            if(sample1 == null || sample2 == null) return false;

            BigDecimal b1 = new BigDecimal(sample1.get(SensingUtils.TIMESTAMP).getAsString());
            BigDecimal b2 = new BigDecimal(sample2.get(SensingUtils.TIMESTAMP).getAsString());

            BigDecimal e = b2.subtract(b1).multiply(SECOND_2_NANOS).abs();

            return (e.compareTo(MAX_TIME_FRAME)<= 0);
        }
    }
}
