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
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.AdaptiveOffloadingManager;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.AdaptiveOffloadingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.exceptions.InvalidRuleSetException;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.EvaluationSupportStorage;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.LearningSupportStorage;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.exceptions.NothingToArchiveException;

/**
 *
 */
public class ScoutPipeline extends BasicPipeline {

    public final static String NAME = "Scout";
    private final static String LOG_TAG = "ScoutPipeline";

    public final static int PIPELINE_VERSION = 1;

    private String samplingTag = "scout";

    public final static String ACTION_PROFILE = "profile";

    //Mobile Sensing Pipeline
    private MobileSensing mPipeline;
    private ScoutStorageManager storage;

    //Profiling
    private AdaptiveOffloadingManager offloadingManager;

    private ActiveGeoTagger geoTagger;

    //Storage
    //  Weka - Learning Storage
    private LearningSupportStorage wekaStorage = LearningSupportStorage.getInstance();
    //  Test - Storage for graph creation
    private EvaluationSupportStorage evaluationStorage = EvaluationSupportStorage.getInstance();

    public ScoutPipeline() throws AdaptiveOffloadingException, InvalidRuleSetException {
        super();
        storage = ScoutStorageManager.getInstance();
        offloadingManager = AdaptiveOffloadingManager.getInstance(ScoutApplication.getContext());
    }


    private boolean isInstantiated = false;
    synchronized private void instantiateScoutSensing() throws AdaptiveOffloadingException {

        if(isInstantiated) return;

        geoTagger = ActiveGeoTagger.getInstance();

        mPipeline = new MobileSensing();
        mPipeline.setWindowSize(3);

        RoadConditionMonitoringPipeline rPipeline = new RoadConditionMonitoringPipeline(
                RoadConditionMonitoringPipeline.generateRoadConditionMonitoringPipelineConfiguration(true, false));


        RoadSlopeMonitoringPipeline sPipeline = new RoadSlopeMonitoringPipeline(
                RoadSlopeMonitoringPipeline.generateRoadSlopeMonitoringPipelineConfiguration(true, false));

        //Scout Profiling
        offloadingManager.validatePipeline(rPipeline);
        offloadingManager.validatePipeline(sPipeline);

        mPipeline.addSensorProcessingPipeline(rPipeline);
        mPipeline.addSensorProcessingPipeline(sPipeline);

        offloadingManager.optimizePipelines();

        isInstantiated = true;
    }

    @Override
    public void onCreate(FunfManager manager) {
        super.onCreate(manager);

        this.setName(NAME);

        try {
            instantiateScoutSensing();
            mPipeline.startSensingSession();
            wekaStorage.prepareStorage();
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
                }catch (Exception e){
                    e.printStackTrace();
                }

                if(offloadingManager.isProfilingEnabled())
                    offloadingManager.exportOffloadingLog();

                break;
            case ACTION_PROFILE:
                /* DEPRECATED
                if(offloadingManager.isProfilingEnabled()) {
                    if (offloadingManager.isProfiling())
                        offloadingManager.stopProfiling();
                    else
                        offloadingManager.startProfiling();
                }*/
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
            jsonData.addProperty(SensingUtils.GeneralFields.SENSOR_TYPE, sensorType);
            jsonData.addProperty(SensingUtils.GeneralFields.SCOUT_TIME, System.nanoTime());
        } catch (MobileSensingException e) {
            e.printStackTrace();
            return;
        }

        switch (sensorType){
            case SensingUtils.Sensors.LOCATION:
                geoTagger.pushLocation(jsonData);
                break;
            case SensingUtils.Sensors.ROTATION_VECTOR:
                geoTagger.pushOrientation(jsonData);
                break;
            default:
                geoTagger.tagSample(jsonData);
                mPipeline.pushSensorSample(jsonData);
        }
    }

    @Override
    public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
        if(probeConfig!=null) Log.v(LOG_TAG, "[CONFIG]: "+probeConfig.toString());
    }

    @Override
    public void onDestroy() {
        offloadingManager.resetPartitionEngine();
        mPipeline.stopSensingSession();
        wekaStorage.teardown();
        evaluationStorage.teardown();
        super.onDestroy();
    }

    public void setSamplingTag(String samplingTag) {
        this.samplingTag = samplingTag;
    }
}