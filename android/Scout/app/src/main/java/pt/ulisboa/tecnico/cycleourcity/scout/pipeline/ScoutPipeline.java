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

        JsonObject  jsonData = data.getAsJsonObject(),
                    jsonConfig = probeConfig.getAsJsonObject();

        int sensorType = 0;

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


    public void testOrientation(IJsonObject data){

        float[] rotationMatrix = new float[9];
        float[] rotationVector = new float[4];

        rotationVector[0] = data.get("xSinThetaOver2").getAsFloat();
        rotationVector[1] = data.get("ySinThetaOver2").getAsFloat();
        rotationVector[2] = data.get("zSinThetaOver2").getAsFloat();
        rotationVector[3] = data.get("cosThetaOver2").getAsFloat();

        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);

        int worldAxisX = SensorManager.AXIS_X;
        int worldAxisY = SensorManager.AXIS_Y;
        int worldAxisZ = SensorManager.AXIS_Z;
        int worldminusX= SensorManager.AXIS_MINUS_X;

        float[] adjustedRotationMatrix = new float[9];

        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisY, worldminusX, adjustedRotationMatrix);

        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);
        float pitch = orientation[1] * -57;
        float roll = orientation[2] * -57;

        Log.d("ORIENTATION", "Pitch="+pitch+" Roll="+roll);



    }

    public void setDisplay(Display display){
        this.display = display;


    }

}