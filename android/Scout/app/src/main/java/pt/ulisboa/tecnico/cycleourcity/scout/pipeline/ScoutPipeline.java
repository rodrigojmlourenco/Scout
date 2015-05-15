package pt.ulisboa.tecnico.cycleourcity.scout.pipeline;

import android.hardware.SensorManager;
import android.util.Log;
import android.view.Display;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.sql.SQLException;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;

import pt.ulisboa.tecnico.cycleourcity.scout.ScoutApplication;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.MobileSensingPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exceptions.MobileSensingException;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.exceptions.NothingToArchiveException;

/**
 *
 */
public class ScoutPipeline extends BasicPipeline {

    private final static String LOG_TAG = "ScoutPipeline";
    public final static String NAME = "Scout";

    private String samplingTag = "scout";

    private Display display;

    //Mobile Sensing Pipeline
    private MobileSensingPipeline mPipeline = MobileSensingPipeline.getInstance();

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
                    mPipeline.archiveData(samplingTag);
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (NothingToArchiveException e) {
                    e.printStackTrace();
                    //TODO: TOAST na main Activity
                }
                break;
            default:
                Log.e(LOG_TAG, "ScoutPipeline doesn't support the "+action+" action.");
        }
    }

    @Override
    public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {


        Log.d("BATTERY", String.valueOf(data));
        /*
        JsonObject  jsonData = data.getAsJsonObject(),
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
        */
    }

    @Override
    public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
        if(probeConfig!=null) Log.v(LOG_TAG, "[CONFIG]: "+probeConfig.toString());
    }

    @Override
    public void onDestroy() {
        mPipeline.stopSensingSession();
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