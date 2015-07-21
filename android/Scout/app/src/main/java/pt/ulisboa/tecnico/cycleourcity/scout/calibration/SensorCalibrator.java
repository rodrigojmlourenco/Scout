package pt.ulisboa.tecnico.cycleourcity.scout.calibration;

import android.content.SharedPreferences;
import android.hardware.SensorEventListener;

import com.google.gson.JsonObject;

/**
 * Created by rodrigo.jm.lourenco on 20/07/2015.
 */
public interface SensorCalibrator {

    public final static String PREFERENCES_NAME = "accelerationOffsets";

    // The total sample size for each state measurement.
    public final static int SAMPLE_SIZE = 1024;

    public void addSample(JsonObject sample);

    public boolean isComplete();

    public boolean successfulCalibration();

    public JsonObject getOffsets();

    public String dumpInfo();

    public void restart();

    public void storeOffsets(SharedPreferences preferences);
}
