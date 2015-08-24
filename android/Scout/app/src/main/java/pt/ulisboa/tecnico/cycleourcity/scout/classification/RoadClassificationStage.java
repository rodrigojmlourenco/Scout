package pt.ulisboa.tecnico.cycleourcity.scout.classification;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import pt.ulisboa.tecnico.cycleourcity.scout.classification.exceptions.InvalidFeatureVectorException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.pipeline.RoadConditionMonitoringPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.ClassificationEvaluationStorage;

public abstract class RoadClassificationStage implements Stage {

    protected final String CLASSIFICATION = "pavement"; //TODO: migrar esta constante para o SensingUtils

    //Testing
    protected String NAME;
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

        if(input == null) return; //Avoid NullPointerException

        for(int i=0; i < input.length; i++)
            try {
                JsonObject aux = generateClassification(input[i]);
                storage.registerClassification(NAME, aux.get(CLASSIFICATION).getAsString());
            } catch (InvalidFeatureVectorException e) {
                e.printStackTrace();
            }
    }
}
