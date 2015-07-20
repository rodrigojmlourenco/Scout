package pt.ulisboa.tecnico.cycleourcity.scout.calibration;

import android.content.Intent;
import android.content.SharedPreferences;

import com.google.gson.JsonObject;

import pt.ulisboa.tecnico.cycleourcity.scout.CalibrateActivity;
import pt.ulisboa.tecnico.cycleourcity.scout.calibration.exceptions.NotYetCalibratedException;
import pt.ulisboa.tecnico.cycleourcity.scout.calibration.exceptions.UninitializedException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;

/**
 * Created by rodrigo.jm.lourenco on 20/07/2015.
 */
public class ScoutCalibrationManager {

    private static ScoutCalibrationManager INSTANCE = null;
    private JsonObject offsets = null;

    private ScoutCalibrationManager(SharedPreferences calibrationPrefs)
            throws NotYetCalibratedException{

        if(calibrationPrefs == null ||
            !calibrationPrefs.getBoolean(LinearAccelerationCalibrator.CalibrationKeys.CALIBRATED, false)){
            throw new NotYetCalibratedException();
        }

        offsets = new JsonObject();
        float x, y, z;

        x = calibrationPrefs.getFloat(LinearAccelerationCalibrator.CalibrationKeys.X_OFFSET, 0.0f);
        y = calibrationPrefs.getFloat(LinearAccelerationCalibrator.CalibrationKeys.Y_OFFSET, 0.0f);
        z = calibrationPrefs.getFloat(LinearAccelerationCalibrator.CalibrationKeys.Z_OFFSET, 0.0f);

        offsets.addProperty(SensingUtils.MotionKeys.X, x);
        offsets.addProperty(SensingUtils.MotionKeys.Y, y);
        offsets.addProperty(SensingUtils.MotionKeys.Z, z);




    }

    public static void initScoutCalibrationManager(SharedPreferences calibrationPrefs)
            throws NotYetCalibratedException {
        if(INSTANCE == null) {
            synchronized (ScoutCalibrationManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ScoutCalibrationManager(calibrationPrefs);
                }
            }
        }
    }

    public static ScoutCalibrationManager getInstance()
            throws UninitializedException{

        if(INSTANCE == null){
            synchronized (ScoutCalibrationManager.class){
                if(INSTANCE==null) throw new UninitializedException();
            }
        }

        return INSTANCE;
    }

    public void tagLinearAccelerationOffsets(JsonObject sample){
        sample.add(SensingUtils.MotionKeys.CALIBRATION, offsets);
    }
}
