package pt.ulisboa.tecnico.cycleourcity.scout.pipeline;

import android.util.Log;

import com.google.gson.JsonElement;

import java.sql.SQLException;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;

import pt.ulisboa.tecnico.cycleourcity.scout.ScoutApplication;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.MobileSensingPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.parser.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.MobileSensingException;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.exceptions.NothingToArchiveException;

/**
 *
 */
public class ScoutPipeline extends BasicPipeline {

    private final static String LOG_TAG = "ScoutPipeline";
    public final static String NAME = "Scout";

    private String samplingTag = "scout";

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

        try {
            mPipeline.pushSensorSample(probeConfig, data);
        } catch (MobileSensingException e) {
            Log.e(LOG_TAG, "Sensor "+probeConfig.get(SensingUtils.SENSOR_TYPE)+" not yet supported by MobileSensingPipeline.");
        }
    }

    @Override
    public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
        //super.onDataCompleted(probeConfig, checkpoint);

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
}