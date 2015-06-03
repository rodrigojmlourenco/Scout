package pt.ulisboa.tecnico.cycleourcity.scout.pipeline;

import android.util.Log;
import android.view.Display;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.ScoutApplication;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.MobileSensing;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exceptions.MobileSensingException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.PipelineConfiguration;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.ConfigurationCaretaker;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.location.LocationSensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.location.PressureSensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.stages.CommonStages;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.AdaptiveOffloadingManager;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.AdaptiveOffloadingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.ProfilingStageWrapper;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.exceptions.NothingToArchiveException;

/**
 *
 */
public class ScoutPipeline extends BasicPipeline {

    private final static String LOG_TAG = "ScoutPipeline";
    public final static String NAME = "Scout";

    private String samplingTag = "scout";

    private Display display;

    public final static String ACTION_PROFILE = "profile";

    //Mobile Sensing Pipeline
    private MobileSensing mPipeline;
    private ScoutStorageManager storage;

    //Profiling
    private final AdaptiveOffloadingManager offloadingManager;

    public ScoutPipeline() throws AdaptiveOffloadingException {
        super();

        mPipeline = new MobileSensing();
        storage = ScoutStorageManager.getInstance();

        //Pipeline Setup
        //  //Location Pipeline
        ConfigurationCaretaker locationCaretaker = new ConfigurationCaretaker();
        PipelineConfiguration locationConfig = new PipelineConfiguration();

        //locationConfig.addStage(new LocationSensorPipeline.TrimStage());
        //locationConfig.addStage(new ProfiledTrimStage());
        locationConfig.addStage(new ProfilingStageWrapper(new LocationSensorPipeline.TrimStage()));
        locationConfig.addStage(new ProfilingStageWrapper(new CommonStages.HeuristicsAdmissionControlStage()));

        //locationConfig.addFinalStage(new CommonStages.TagConfigurationStage(locationCaretaker));
        locationConfig.addFinalStage(new CommonStages.FinalizeStage());
        locationConfig.addFinalStage(new LocationSensorPipeline.UpdateScoutStateStage());
        locationConfig.addFinalStage(new CommonStages.FeatureStorageStage(storage));

        locationCaretaker.setOriginalPipelineConfiguration(locationConfig);

        LocationSensorPipeline lPipeline = new LocationSensorPipeline(locationCaretaker);
        lPipeline.registerCallback(mPipeline.getStateUpdateCallback());

        //  //Pressure Pipeline
        ConfigurationCaretaker pressureCaretaker = new ConfigurationCaretaker();
        PipelineConfiguration pressureConfig = new PipelineConfiguration();

        pressureConfig.addStage(new CommonStages.MergeStage(new PressureSensorPipeline.PressureMergeStrategy()));
        pressureConfig.addStage(new PressureSensorPipeline.FeatureExtractionStage());
        pressureConfig.addFinalStage(new CommonStages.FinalizeStage());
        pressureConfig.addFinalStage(new CommonStages.FeatureStorageStage(storage));
        pressureCaretaker.setOriginalPipelineConfiguration(pressureConfig);
        PressureSensorPipeline pPipeline = new PressureSensorPipeline(pressureCaretaker);
        pPipeline.registerCallback(mPipeline.getStateUpdateCallback());

        mPipeline.addSensorProcessingPipeline(lPipeline);
        mPipeline.addSensorProcessingPipeline(pPipeline);


        //Scout Profiling
        offloadingManager = AdaptiveOffloadingManager.getInstance(ScoutApplication.getContext());
        offloadingManager.setDecisionEngineApathy((float) .125);

        //TODO: enable
        //offloadingManager.validatePipeline(locationConfig);
        //offloadingManager.validatePipeline(pressureConfig);

    }

    @Override
    public void onCreate(FunfManager manager) {
        super.onCreate(manager);

        this.setName(NAME);

        mPipeline.startSensingSession();

        this.reloadDbHelper(ScoutApplication.getContext());
    }

    @Override
    public void onRun(String action, JsonElement config) {
        Log.d(LOG_TAG, "Perform: " + action);

        switch (action){
            case BasicPipeline.ACTION_ARCHIVE:
                try {
                    storage.archive(samplingTag);
                } catch (NothingToArchiveException e) {
                    e.printStackTrace();
                }
                break;
            case ACTION_PROFILE:
                if(offloadingManager.isProfiling())
                    offloadingManager.stopProfiling();
                else
                    offloadingManager.startProfiling();
                break;
            default:
                Log.e(LOG_TAG, "ScoutPipeline doesn't support the "+action+" action.");
        }
    }

    @Override
    public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {

        JsonObject jsonData = data.getAsJsonObject(),
                    jsonConfig = probeConfig.getAsJsonObject();

        int sensorType;

        try {

            sensorType = SensingUtils.getSensorType(jsonConfig);
            jsonData.addProperty(SensingUtils.SENSOR_TYPE, sensorType);
            jsonData.addProperty(SensingUtils.SCOUT_TIME, System.nanoTime());

            mPipeline.pushSensorSample(jsonData);

        } catch (MobileSensingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
        if(probeConfig!=null) Log.v(LOG_TAG, "[CONFIG]: "+probeConfig.toString());
    }

    @Override
    public void onDestroy() {
        mPipeline.stopSensingSession();
        offloadingManager.onDestroy();
        super.onDestroy();
        Log.e(LOG_TAG, "DESTROY!!!");
    }

    public void setSamplingTag(String samplingTag) {
        this.samplingTag = samplingTag;
    }

    public void setDisplay(Display display){
        this.display = display;
    }

}