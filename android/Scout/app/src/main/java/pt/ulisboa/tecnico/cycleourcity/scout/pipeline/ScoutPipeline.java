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
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.location.LocationPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.location.LocationSensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.stages.CommonStages;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.ScoutProfiler;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.ProfiledTrimStage;
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
    private final ScoutProfiler profiler;

    public ScoutPipeline(){
        super();

        mPipeline = new MobileSensing();
        storage = ScoutStorageManager.getInstance();


        ConfigurationCaretaker locationCaretaker = new ConfigurationCaretaker();
        PipelineConfiguration locationConfig = new PipelineConfiguration();

        //locationConfig.addStage(new LocationSensorPipeline.TrimStage());
        //locationConfig.addStage(new ProfiledTrimStage());
        locationConfig.addStage(new ProfilingStageWrapper(new LocationSensorPipeline.TrimStage()));
        locationConfig.addStage(new ProfilingStageWrapper(new CommonStages.HeuristicsAdmissionControlStage()));

        locationConfig.addFinalStage(new CommonStages.TagConfigurationStage(locationCaretaker));
        locationConfig.addFinalStage(new CommonStages.FinalizeStage());
        locationConfig.addFinalStage(new LocationSensorPipeline.UpdateScoutStateStage());
        locationConfig.addFinalStage(new CommonStages.FeatureStorageStage(storage));

        locationCaretaker.setOriginalPipelineConfiguration(locationConfig);

        LocationSensorPipeline lPipeline = new LocationSensorPipeline(locationCaretaker);
        lPipeline.registerCallback(mPipeline.getStateUpdateCallback());
        mPipeline.addSensorProcessingPipeline(lPipeline);

        //Scout Profiling
        this.profiler = new ScoutProfiler(
                ScoutApplication.getContext(),
                mPipeline.getMobileSensingState());
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
                if (profiler.isProfiling())
                    profiler.stopProfiling();
                else
                    profiler.startProfiling();
                break;
            default:
                Log.e(LOG_TAG, "ScoutPipeline doesn't support the "+action+" action.");
        }
    }

    @Override
    public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {

        Log.d("TESTING", String.valueOf(mPipeline.getMobileSensingState().isSensing()));

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
        profiler.stopProfiling();
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