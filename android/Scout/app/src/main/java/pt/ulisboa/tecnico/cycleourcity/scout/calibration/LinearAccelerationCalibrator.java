package pt.ulisboa.tecnico.cycleourcity.scout.calibration;

import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import com.google.gson.JsonObject;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain.EnvelopeMetrics;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain.RMS;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain.StatisticalMetrics;

/**
 * Created by rodrigo.jm.lourenco on 20/07/2015.
 */
public class LinearAccelerationCalibrator implements SensorCalibrator{

    public final float VARIANCE_THRESHOLD = 0.04f;

    //Logging
    private final String LOG_TAG = "LA_Calibrator";
    private final boolean VERBOSE = true;

    private int currentSample = 0;
    private final double[]  x = new double[SAMPLE_SIZE],
                            y = new double[SAMPLE_SIZE],
                            z = new double[SAMPLE_SIZE];

    private boolean offsetsCalculated = false;
    private double xMean, yMean, zMean;
    private double xMedian, yMedian, zMedian;
    private double xVariance, yVariance, zVariance;
    private double xStdDev, yStdDev, zStdDev;
    private double xRMS, yRMS, zRMS;


    @Override
    public void addSample(JsonObject sample) {
        if(currentSample < SAMPLE_SIZE) {
            x[currentSample] = sample.get(SensingUtils.MotionKeys.X).getAsDouble();
            y[currentSample] = sample.get(SensingUtils.MotionKeys.Y).getAsDouble();
            z[currentSample] = sample.get(SensingUtils.MotionKeys.Z).getAsDouble();
            currentSample++;

        }else calculateOffsets();
    }

    @Override
    public boolean isComplete() {
        return currentSample == SAMPLE_SIZE;
    }

    @Override
    public boolean successfulCalibration() {

        if(isComplete()) calculateOffsets();

        boolean valid = offsetsCalculated &&
                xVariance < VARIANCE_THRESHOLD &&
                yVariance < VARIANCE_THRESHOLD &&
                zVariance < VARIANCE_THRESHOLD;

        return valid;
    }

    @Override
    public JsonObject getOffsets() {

        if(!offsetsCalculated)calculateOffsets();

        JsonObject offsets = new JsonObject();
        offsets.addProperty(SensingUtils.MotionKeys.X, xMean);
        offsets.addProperty(SensingUtils.MotionKeys.Y, yMean);
        offsets.addProperty(SensingUtils.MotionKeys.Z, zMean);
        return offsets;
    }

    @Override
    public String dumpInfo() {
        return  "Calibration [Accelerometer]\n"+
                "\tMean\t\t(x|y|z):\t\t"+String.format("%.4f",xMean)+" | "+String.format("%.4f",+yMean)+" | "+String.format("%.4f",zMean)+"\n"+
                "\tMedian\t\t(x|y|z):\t\t"+String.format("%.4f",xMedian)+" | "+String.format("%.4f,",yMedian)+" | "+String.format("%.4f,",zMedian)+"\n"+
                "\tVariance\t(x|y|z):\t\t"+String.format("%.4f,",xVariance)+" | "+String.format("%.4f,",yVariance)+" | "+String.format("%.4f,",zVariance)+"\n"+
                "\tStd Dev\t\t(x|y|z):\t\t"+String.format("%.4f,",xStdDev)+" | "+String.format("%.4f,",yStdDev)+" | "+String.format("%.4f,",zStdDev)+"\n"+
                "\tRMS\t\t\t(x|y|z):\t\t"+String.format("%.4f,",xRMS)+" | "+String.format("%.4f,",yRMS)+" | "+String.format("%.4f,",zRMS);
    }


    private void calculateOffsets(){
        if(!offsetsCalculated){
            //Required
            xMean = StatisticalMetrics.calculateMean(x);
            yMean = StatisticalMetrics.calculateMean(y);
            zMean = StatisticalMetrics.calculateMean(z);

            //Optional
            xMedian = EnvelopeMetrics.calculateMedian(x);
            yMedian = EnvelopeMetrics.calculateMedian(y);
            zMedian = EnvelopeMetrics.calculateMedian(z);

            xVariance = StatisticalMetrics.calculateVariance(x);
            yVariance = StatisticalMetrics.calculateVariance(y);
            zVariance = StatisticalMetrics.calculateVariance(z);

            xStdDev = StatisticalMetrics.calculateStandardDeviation(x);
            yStdDev = StatisticalMetrics.calculateStandardDeviation(y);
            zStdDev = StatisticalMetrics.calculateStandardDeviation(z);

            xRMS = RMS.calculateRootMeanSquare(x);
            yRMS = RMS.calculateRootMeanSquare(y);
            zRMS = RMS.calculateRootMeanSquare(z);

            offsetsCalculated = true;

            if(VERBOSE) {
                Log.d(LOG_TAG, "Calculating the offsets");
                Log.d(LOG_TAG, dumpInfo());
            }
        }
    }

    @Override
    public void restart() {
        currentSample = 0;
        offsetsCalculated = false;
    }

    @Override
    public void storeOffsets(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(LinearAccelerationCalibrationKeys.CALIBRATED, true);
        editor.putFloat(LinearAccelerationCalibrationKeys.X_OFFSET, (float) xMean);
        editor.putFloat(LinearAccelerationCalibrationKeys.Y_OFFSET, (float) yMean);
        editor.putFloat(LinearAccelerationCalibrationKeys.Z_OFFSET, (float) zMean);
        editor.commit();
    }

    public static interface LinearAccelerationCalibrationKeys{
        public final static String
                CALIBRATED = "LinearAcceleration",
                X_OFFSET = "xOffset",
                Y_OFFSET = "yOffset",
                Z_OFFSET = "zOffset";
    }
}
