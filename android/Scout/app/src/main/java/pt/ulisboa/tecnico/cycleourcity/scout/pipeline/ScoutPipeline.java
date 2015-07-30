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
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.SensorProcessingPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.AdaptiveOffloadingManager;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.AdaptiveOffloadingException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.exceptions.InvalidRuleSetException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.ConfigurationTaggingStage;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.TestOffloadingStage;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.TestStages;
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

        /* Commented for Testing purposes
        RoadConditionMonitoringPipeline rPipeline = new RoadConditionMonitoringPipeline(
                RoadConditionMonitoringPipeline.generateRoadConditionMonitoringPipelineConfiguration());
        mPipeline.addSensorProcessingPipeline(rPipeline);


        RoadSlopeMonitoringPipeline sPipeline = new RoadSlopeMonitoringPipeline(
                RoadSlopeMonitoringPipeline.generateRoadSlopeMonitoringPipelineConfiguration(true));
        mPipeline.addSensorProcessingPipeline(sPipeline);
        */


        PipelineConfiguration pc1 = new PipelineConfiguration();

        pc1.addStage(new TestOffloadingStage("pc11", new TestStages.Test6000Stage(), 6000, 700, 600));
        pc1.addStage(new TestOffloadingStage("pc12", new TestStages.Test4000Stage(), 4000, 600, 500));
        pc1.addFinalStage(new ConfigurationTaggingStage());
        SensorProcessingPipeline p1 = new SensorProcessingPipeline(SensingUtils.Sensors.LINEAR_ACCELERATION, pc1) {};

        /*
        PipelineConfiguration pc2 = new PipelineConfiguration();
        pc2.addStage(new OffloadingStageWrapper("pc21", new TestStages.Test6000Stage()));
        //pc2.addStage(new OffloadingStageWrapper(new TestStages.Test5000Stage()));
        pc2.addStage(new TestOffloadingStage(new TestStages.Test5000Stage(), 5000, 300, 500, "pc22"));
        pc2.addFinalStage(new ConfigurationTaggingStage());
        SensorProcessingPipeline p2 = new SensorProcessingPipeline(SensingUtils.Sensors.PRESSURE, pc2) {};
        */

        //Scout Profiling
        offloadingManager.validatePipeline(p1);
        mPipeline.addSensorProcessingPipeline(p1);

        //offloadingManager.validatePipeline(p2);
        //mPipeline.addSensorProcessingPipeline(p2);
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
        mPipeline.stopSensingSession();
        wekaStorage.teardown();
        evaluationStorage.teardown();
        super.onDestroy();
    }

    public void setSamplingTag(String samplingTag) {
        this.samplingTag = samplingTag;
    }
}