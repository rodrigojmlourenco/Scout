package pt.ulisboa.tecnico.cycleourcity.scout.pipeline.stages.classification;

import android.content.Context;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import pt.ulisboa.tecnico.cycleourcity.scout.ScoutApplication;
import pt.ulisboa.tecnico.cycleourcity.scout.pipeline.stages.classification.exceptions.InvalidFeatureVectorException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.pipeline.RoadConditionMonitoringPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.ClassificationEvaluationStorage;

public abstract class RoadClassificationStage implements Stage {

    protected final String CLASSIFICATION = "pavement"; //TODO: migrar esta constante para o SensingUtils

    //Testing
    protected String NAME;
    private Context ctx = ScoutApplication.getContext();
    private final ClassificationEvaluationStorage storage = ClassificationEvaluationStorage.getInstance();

    //Logging
    protected final boolean VERBOSE= true;
    protected final String LOG_TAG = "RoadClassification";

    abstract JsonObject generateClassification(JsonObject featureVector)
        throws InvalidFeatureVectorException;

    protected final JsonObject generateClassificationStub(JsonObject featureVector){
        JsonObject classification = new JsonObject();

        classification.addProperty(SensingUtils.GeneralFields.SENSOR_TYPE, RoadConditionMonitoringPipeline.SENSOR_TYPE);
        classification.addProperty(SensingUtils.GeneralFields.TIMESTAMP, featureVector.get(SensingUtils.GeneralFields.TIMESTAMP).getAsString());
        classification.addProperty(SensingUtils.GeneralFields.SCOUT_TIME, System.nanoTime());

        classification.add(SensingUtils.LocationKeys.LOCATION, featureVector.get(SensingUtils.LocationKeys.LOCATION));

        return classification;
    }

    @Override
    public final void execute(PipelineContext pipelineContext) {
        SensorPipelineContext ctx = (SensorPipelineContext)pipelineContext;
        JsonObject[] input = ctx.getInput();
        JsonObject[] output= new JsonObject[input.length];

        if(input == null) return; //Avoid NullPointerException

        long start, stop;

        for(int i=0; i < input.length; i++)
            try {
                output[i] = generateClassification(input[i]);
            } catch (InvalidFeatureVectorException e) {
                e.printStackTrace();
            }

        ctx.setOutput(output);
    }
}
