package pt.ulisboa.tecnico.cycleourcity.evalscout.offloading;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.AdaptivePipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.SensorProcessingPipeline;
import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.exceptions.UnvalidatedPipelineException;
import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.stages.OffloadingWrapperStage;

/**
 * Created by rodrigo.jm.lourenco on 23/07/2015.
 */
public class OffloadTracker {

    public static final String CONFIGURATION = "config";

    private JsonParser jsonParser;
    private ConcurrentHashMap<Integer, LinkedList> missingStages;

    public OffloadTracker(){
        jsonParser = new JsonParser();
        this.missingStages = new ConcurrentHashMap<>();
    }

    public void registerPipeline(AdaptivePipeline pipeline){

        int pID = pipeline.hashCode();

        if(!missingStages.containsKey(pID)) {
            missingStages.put(pID, new LinkedList());
        }

    }

    public void registerPipeline(int pUID){

        if(!missingStages.containsKey(pUID))
            missingStages.put(pUID, new LinkedList());
    }

    public void markOffloadedStage(AdaptivePipeline pipeline, OffloadingWrapperStage stage){

        int pID = pipeline.hashCode();

        if(missingStages.containsKey(pID)){
            missingStages.get(pID).addFirst(stage.getStageClass().getName());
        }
    }

    public void unmarkOffloadedStage(AdaptivePipeline pipeline) {

        int pID = pipeline.hashCode();

        if(missingStages.containsKey(pID))
            missingStages.get(pID).removeFirst();

    }

    @Deprecated
    public void markOffloadedStage(int pUID, OffloadingWrapperStage wrapper){
        if(missingStages.containsKey(pUID)){
            missingStages.get(pUID).add(wrapper.getStageClass().getName());
        }
    }

    public JsonArray getConfiguration(SensorProcessingPipeline pipeline) throws UnvalidatedPipelineException {

        int pID = pipeline.hashCode();

        if(!missingStages.containsKey(pID)) throw new UnvalidatedPipelineException();

        JsonArray configuration = new JsonArray();
        LinkedList<String> missing = missingStages.get(pID);

        for(String stage : missing){
            configuration.add(jsonParser.parse(stage));
        }

        return configuration;
    }

    public JsonArray getConfiguration(int pUID) throws UnvalidatedPipelineException {

        if(!missingStages.containsKey(pUID)) throw new UnvalidatedPipelineException();

        JsonArray configuration = new JsonArray();
        LinkedList<String> missing = missingStages.get(pUID);

        for(String stage : missing){
            configuration.add(jsonParser.parse(stage));
        }

        if(missing.size() <= 0)
            return null;
        else
            return configuration;
    }

    protected void teardown(){
        missingStages.clear();
    }
}
