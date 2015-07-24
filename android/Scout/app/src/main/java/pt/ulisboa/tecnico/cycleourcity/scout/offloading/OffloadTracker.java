package pt.ulisboa.tecnico.cycleourcity.scout.offloading;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.ideaimpl.patterns.pipeline.Stage;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.AdaptivePipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.ConfigurationCaretaker;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.SensorProcessingPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.UnvalidatedPipelineException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.OffloadingStageWrapper;

/**
 * Created by rodrigo.jm.lourenco on 23/07/2015.
 */
public class OffloadTracker {

    public static final String CONFIGURATION = "config";

    private JsonParser jsonParser;
    private ConcurrentHashMap<Integer, LinkedList> missingStages;
    private ConcurrentHashMap<Integer, ConfigurationCaretaker> configurationCaretakers;

    public OffloadTracker(){
        jsonParser = new JsonParser();

        this.missingStages = new ConcurrentHashMap<>();
        this.configurationCaretakers = new ConcurrentHashMap<>();
    }

    public void registerPipeline(AdaptivePipeline pipeline){

        int pID = pipeline.hashCode();

        if(!missingStages.containsKey(pID)) {
            missingStages.put(pID, new LinkedList());

            //ConfigurationCaretaker caretaker = new ConfigurationCaretaker();
            //caretaker.setOriginalPipelineConfiguration(pipeline.get);
            //configurationCaretakers.put(pID,)
        }

    }

    public void registerPipeline(int pUID){

        if(!missingStages.containsKey(pUID))
            missingStages.put(pUID, new LinkedList());
    }

    public void markOffloadedStage(AdaptivePipeline pipeline, OffloadingStageWrapper stage){

        int pID = pipeline.hashCode();

        if(missingStages.containsKey(pID)){
            missingStages.get(pID).addFirst(stage.getStageClass().getName());
        }
    }

    public void markOffloadedStage(int pUID, OffloadingStageWrapper wrapper){
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
}
