package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.pipelines;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import pt.ulisboa.tecnico.cycleourcity.scout.ScoutApplication;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.OffloadingWrapperStage;
import pt.ulisboa.tecnico.cycleourcity.scout.pipeline.ScoutPipeline;

/**
 * This is a singleton that is responsible for managing all information regarding
 * the characteristics of running stages, belonging to active pipelines.
 */
public class StageProfiler {

    //Logging
    public final static boolean VERBOSE = true;
    public final static String LOG_TAG ="StageProfiling";

    //Profiling configuration parameters
    public static final Integer NUM_PROFILING_SAMPLES = 512+1; //Make it odd for median

    //Singleton
    private static StageProfiler STAGE_PROFILER = null;

    private boolean hasModel = false;
    private final Context appContext;
    private HashMap<String, OffloadingWrapperStage> stages; //TODO: this is used for trackability, but a simpler structure would be better
    private ConcurrentHashMap<String, StageModel> modelledStages;

    private StageProfiler(){
        stages = new HashMap<>();
        modelledStages = new ConcurrentHashMap<>();

        appContext = ScoutApplication.getContext();

        hasModel = fetchModel();
    }

    public static StageProfiler getInstance(){

        synchronized (StageProfiler.class) {
            if (STAGE_PROFILER == null) {
                STAGE_PROFILER = new StageProfiler();
            }
        }

        return STAGE_PROFILER;
    }

    /**
     * Registers a stage
     * @param identifier the Stage unique identifier
     * @param stage The Stage wrapper
     * TODO: search for a simple way of tracking this information, has maintaining the wrapper serves no purpose
     */
    public void registerStage(String identifier,OffloadingWrapperStage stage){
        this.stages.put(identifier, stage);
    }

    /**
     * This method generates a StageModel for a given Stage, and if all registered stages
     * have been modelled then the complete model may be generated and stored.
     * <br>
     * If a model already exists and has been successfully fetched then this method is ignored
     * and skipped.
     *
     * @param identifier the stage unique identifier, which is manually defined
     * @param stage the stage canonical name, for offload tracking
     * @param executionTime the stage average execution time TODO: median would be less susceptible to noise
     * @param inputDataSize the stage average inputted data TODO: median would be less susceptible to noise
     * @param outputDataSize the stage average outputted data TODO: median would be less susceptible to noise
     */
    public void generateStageModel(String identifier,
                                   String stage,
                                   long executionTime,
                                   long inputDataSize, long outputDataSize){

        if(hasModel){
            if(VERBOSE) Log.d(LOG_TAG, "A complete model already exists, skipping this method.");
            return;
        }

        modelledStages.put(identifier, new StageModel(identifier, stage, executionTime, inputDataSize, outputDataSize));

        //When all registered stages are associated with a model, the overall configuration
        //module is saved persistently, using the SharedPreferences framework.
        if(modelledStages.size() == stages.size())
            storeModel();
    }

    /**
     * Checks is a given stage has already been modelled.
     * @param identifier the string that uniquely identifies the stage.
     * @return true if the stage has been modelled, false otherwise.
     */
    public boolean hasBeenModeled(String identifier){
        return modelledStages.containsKey(identifier);
    }

    /**
     * Checks if the StageProfiler already has a complete model.
     * @return True if a complete model already exists, false otherwise.
     */
    public boolean hasModel(){return hasModel; }

    /*
     ********************************************************
     * Partition Engine Access Functions                    *
     ********************************************************
     */

    /**
     * Retrieves the average execution time of a given Stage
     * @param stageIdentifier the Stage unique identifier.
     * @return average execution time (long)
     */
    public long getExecutionTime(String stageIdentifier){
        return modelledStages.get(stageIdentifier).executionTime;
    }

    /**
     * Retrieves the average inputted data of a given Stage
     * @param stageIdentifier the Stage unique identified
     * @return average input data size (long)
     */
    public long getInputDataSize(String stageIdentifier){
        return modelledStages.get(stageIdentifier).inputDataSize;
    }

    /**
     * Retrieves the average outputted data of a given Stage
     * @param stageIdentifier the Stage unique identifier
     * @return average output data size (long)
     */
    public long getOutputDataSize(String stageIdentifier){
        return modelledStages.get(stageIdentifier).outputDataSize;
    }

    /**
     * Retrieves the Stage class canonical name, which can be used to employ reflection.
     * @param stageIdentifier the Stage unique identifier
     * @return The Stage class canonical name
     */
    public String getStageCanonicalName(String stageIdentifier){
        return modelledStages.get(stageIdentifier).stage;
    }

    /*
     ********************************************************
     * Model Storage                                        *
     ********************************************************
     */
    interface ModelStorageKeys {
        String
                STORAGE = "stagesModel",
                VERSION = "@version",
                MODEL   = "@model";
    }

    private boolean fetchModel(){

        SharedPreferences prefs =
                appContext.getSharedPreferences(ModelStorageKeys.STORAGE, Context.MODE_PRIVATE);

        String modelAsString = prefs.getString(ModelStorageKeys.MODEL, "");


        if(modelAsString != null && modelAsString.isEmpty()){
            if(VERBOSE) Log.d(LOG_TAG, "No model has been found, a new model has to be created.");
            return false;
        }

        JsonParser parser = new JsonParser();
        JsonObject model = (JsonObject) parser.parse(modelAsString);

        //Check if the pipeline version has changed
        //  if it has changes the current model is cleared and a new one has to be created
        int version = model.get(ModelStorageKeys.VERSION).getAsInt();
        if(version != ScoutPipeline.PIPELINE_VERSION){
            prefs.edit().clear().apply();
            if(VERBOSE) Log.d(LOG_TAG, "The stored model has an old version, a new model has to be created");
            return false;
        }


        JsonArray modelConfig = (JsonArray) model.get(ModelStorageKeys.MODEL);

        for(JsonElement stageModel : modelConfig){
            String identifier = ((JsonObject)stageModel).get(StageModel.IDENTIFIER).getAsString();
            modelledStages.put(identifier, new StageModel(((JsonObject)stageModel)));
        }

        if(VERBOSE) Log.d(LOG_TAG, "A complete model was successfully retrieved from memory - "+model);
        return true;
    }


    private void storeModel(){

        Gson gson = new Gson();
        JsonArray configModel = new JsonArray();
        JsonObject model = new JsonObject();

        for (String stageIdentifier : modelledStages.keySet())
            configModel.add(modelledStages.get(stageIdentifier).dumpState());

        model.addProperty(ModelStorageKeys.VERSION, ScoutPipeline.PIPELINE_VERSION);
        model.add(ModelStorageKeys.MODEL, configModel);

        SharedPreferences prefs =
                appContext.getSharedPreferences(ModelStorageKeys.STORAGE, Context.MODE_PRIVATE);

        prefs.edit()
                .putString(ModelStorageKeys.MODEL, gson.toJson(model))
                .apply();

        if(VERBOSE) Log.d(LOG_TAG, "The complete model has been created and stored. Model: "+model);
    }

    /**
     * StageModel is a class that encapsulates all the information necessary
     * to create a Stage model.
     * <br>
     * That information includes:<br>
     * <ul>
     *     <li>The stage identifier and class name</li>
     *     <li>Execution Times</li>
     *     <li>Sizes of the input and output data</li>
     * </ul>
     */
    private class StageModel {

        public final static String
                IDENTIFIER      = "@identifier",
                STAGE           = "@stage",
                EXECUTION_TIME  = "@execution_time",
                INPUT_DATA_SIZE = "@input_data_size",
                OUTPUT_DATA_SIZE= "@output_data_size";

        final String identifier, stage;
        final long executionTime, inputDataSize, outputDataSize;

        public StageModel(String identifier, String stage,
                          long executionTime,
                          long inputDataSize, long outputDataSize){

            this.identifier     = identifier;
            this.stage          = stage;
            this.executionTime = executionTime;
            this.inputDataSize = inputDataSize;
            this.outputDataSize= outputDataSize;
        }

        public StageModel(JsonObject stageModel) {
            identifier = stageModel.get(StageModel.IDENTIFIER).getAsString();
            stage = stageModel.get(StageModel.STAGE).getAsString();
            executionTime = stageModel.get(StageModel.EXECUTION_TIME).getAsLong();
            inputDataSize = stageModel.get(StageModel.INPUT_DATA_SIZE).getAsLong();
            outputDataSize = stageModel.get(StageModel.OUTPUT_DATA_SIZE).getAsLong();
        }

        public JsonObject dumpState(){
            JsonObject state = new JsonObject();

            state.addProperty(IDENTIFIER, identifier);
            state.addProperty(STAGE, stage);
            state.addProperty(EXECUTION_TIME, executionTime);
            state.addProperty(INPUT_DATA_SIZE, inputDataSize);
            state.addProperty(OUTPUT_DATA_SIZE, outputDataSize);

            return state;
        }
    }
}