package pt.ulisboa.tecnico.cycleourcity.scout.pipeline;

import android.util.Log;

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
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.motion.AccelerometerSensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.stages.CommonStages;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.AdaptiveOffloadingManager;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.OffloadingDecisionEngine;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.AdaptiveOffloadingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.AdaptiveOffloadingTaggingStage;
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

    public final static String ACTION_PROFILE = "profile";

    //Mobile Sensing Pipeline
    private MobileSensing mPipeline;
    private ScoutStorageManager storage;

    //Profiling
    private AdaptiveOffloadingManager offloadingManager;

    public ScoutPipeline() throws AdaptiveOffloadingException {
        super();
        storage = ScoutStorageManager.getInstance();
        offloadingManager = AdaptiveOffloadingManager.getInstance(ScoutApplication.getContext());
        offloadingManager.setDecisionEngineApathy(OffloadingDecisionEngine.RECOMMENDED_APATHY);
    }

    private boolean isInstantiated = false;
    synchronized private void instantiateScoutSensing() throws AdaptiveOffloadingException {

        if(isInstantiated) return;

        mPipeline = new MobileSensing();


        //Pipeline Setup
        //  //Location Pipeline
        ConfigurationCaretaker locationCaretaker = new ConfigurationCaretaker();
        PipelineConfiguration locationConfig = new PipelineConfiguration();
        locationConfig.addStage(new ProfilingStageWrapper(new LocationSensorPipeline.TrimStage()));
        locationConfig.addStage(new ProfilingStageWrapper(new CommonStages.HeuristicsAdmissionControlStage()));
        locationConfig.addFinalStage(new AdaptiveOffloadingTaggingStage(locationCaretaker));
        locationConfig.addFinalStage(new CommonStages.FinalizeStage());
        locationConfig.addFinalStage(new LocationSensorPipeline.UpdateScoutStateStage());
        locationConfig.addFinalStage(new CommonStages.FeatureStorageStage(storage));
        locationCaretaker.setOriginalPipelineConfiguration(locationConfig);
        LocationSensorPipeline lPipeline = new LocationSensorPipeline(locationCaretaker);

        //  //Pressure Pipeline
        ConfigurationCaretaker pressureCaretaker = new ConfigurationCaretaker();
        PipelineConfiguration pressureConfig = new PipelineConfiguration();
        pressureConfig.addStage(new ProfilingStageWrapper(new PressureSensorPipeline.PressureMergeStage()));
        pressureConfig.addStage(new ProfilingStageWrapper(new PressureSensorPipeline.FeatureExtractionStage()));
        pressureConfig.addFinalStage(new AdaptiveOffloadingTaggingStage(pressureCaretaker));
        pressureConfig.addFinalStage(new CommonStages.FinalizeStage());
        pressureConfig.addFinalStage(new CommonStages.FeatureStorageStage(storage));
        pressureCaretaker.setOriginalPipelineConfiguration(pressureConfig);
        PressureSensorPipeline pPipeline = new PressureSensorPipeline(pressureCaretaker);

        //  //Accelerometer Pipeline
        ConfigurationCaretaker accelerometerCaretaker = new ConfigurationCaretaker();
        PipelineConfiguration accelerometerConfig = new PipelineConfiguration();
        accelerometerConfig.addStage(new ProfilingStageWrapper(new AccelerometerSensorPipeline.AdmissionControlStage()));
        //accelerometerConfig.addStage(new ProfilingStageWrapper(new AccelerometerSensorPipeline.TrimStage()));
        accelerometerConfig.addStage(new ProfilingStageWrapper(new AccelerometerSensorPipeline.FeatureExtractionStage()));
        accelerometerConfig.addFinalStage(new AdaptiveOffloadingTaggingStage(accelerometerCaretaker));
        accelerometerConfig.addFinalStage(new CommonStages.FinalizeStage());
        accelerometerConfig.addFinalStage(new CommonStages.FeatureStorageStage(storage));
        accelerometerCaretaker.setOriginalPipelineConfiguration(accelerometerConfig);
        AccelerometerSensorPipeline aPipeline = new AccelerometerSensorPipeline(accelerometerCaretaker);

        mPipeline.addSensorProcessingPipeline(lPipeline);
        mPipeline.addSensorProcessingPipeline(pPipeline);
        mPipeline.addSensorProcessingPipeline(aPipeline);


        //Scout Profiling
        offloadingManager.validatePipeline(lPipeline);
        offloadingManager.validatePipeline(pPipeline);

        isInstantiated = true;
    }

    @Override
    public void onCreate(FunfManager manager) {
        super.onCreate(manager);

        this.setName(NAME);

        try {
            instantiateScoutSensing();
            mPipeline.startSensingSession();
        } catch (AdaptiveOffloadingException e) {
            e.printStackTrace();
        }


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

                offloadingManager.exportOffloadingLog();

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
        super.onDestroy();
    }

    public void setSamplingTag(String samplingTag) {
        this.samplingTag = samplingTag;
    }
}