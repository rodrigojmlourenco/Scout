package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.stages;

import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.mit.media.funf.math.FFT;
import edu.mit.media.funf.math.Window;
import edu.mit.media.funf.probe.Probe;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.data.AccelerometerSampleWindow;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeLineContext;

/**
 * Created by rodrigo.jm.lourenco on 15/03/2015.
 * TODO: abordar a gravidade, ver para mais informação http://developer.android.com/guide/topics/sensors/sensors_motion.html#sensors-motion-accel
 */
public class FeatureExtractionStage extends Probe.Base implements Stage {

    private final static String LOG_TAG = "FeatureExtractionStage";

    //keys
    public final static String SENSOR = "Sensor";
    public final static String SENSOR_TYPE = "Accelerometer";

    public final static String MEAN = "Mean";
    public final static String STANDARD_DEVIATION = "Standard Deviation";
    public final static String MAX_DEVIATION = "Max Deviation";
    public final static String ABSOLUTE_CENTRAL_MOMENT = "Abs Central Moment";
    public final static String PSD_ACROSS_FREQUENCY_BANDS = "PSD Accoss Freq Bands";


    //Config Key Values
    //TODO: should be configurable
    private int fftSize = 128;
    private double[] freqBandEdges = {0,1,3,6,10};


    private double[][] frameBuffer = null;
    private double[] fftBufferR = null;
    private double[] fftBufferI = null;

    private FFT featureFFT = null;
    private Window featureWin = null;
    private static int[] freqBandIdx = null;

    private JsonObject output = new JsonObject();

    /**
     * Calculates the mean value of the specified axis values
     * @param samples Accelerometer samples
     * @param frameSamples Total number of samples
     * @param axis The axis (0-x; 1-y; 2-z)
     * @return
     */
    private double calculateMean(double[][] samples, int frameSamples, int axis){
        double mean = 0;

        assert axis >=0 && axis <= 2;

        for(int i=0; i < frameSamples; i++)
            mean += samples[i][axis];

        mean /= (double)frameSamples;

        return mean;
    }

    private double calculateAbsoluteCentralMoment(double[][] samples, int frameSamples, int axis, double mean){
        double accum = 0;

        for(int i=0; i < frameSamples; i++)
            accum += Math.abs(samples[i][axis] - mean);

        accum /= (double) frameSamples;

        return accum;
    }

    private double calculateAbsoluteCentralMoment(double[][] samples, int frameSamples, int axis){
        double mean = calculateMean(samples, frameSamples, axis);
        return calculateAbsoluteCentralMoment(samples, frameSamples, axis, mean);
    }

    private double calculateStandardDeviation(double[][] samples, int frameSamples, int axis, double mean){
        double standardDeviation = 0;

        for(int i=0; i < frameSamples; i++)
            standardDeviation += (samples[i][axis]-mean)*(samples[i][axis]-mean); //SQUARE

        standardDeviation = Math.sqrt(standardDeviation/(double) frameSamples);

        return standardDeviation;
    }

    private double calculateStandardDeviation(double[][] samples, int frameSamples, int axis){
        double mean = calculateMean(samples, frameSamples, axis);
        return calculateStandardDeviation(samples, frameSamples, axis, mean);
    }

    private double calculateMaxDeviation(double[][] samples, int frameSamples, int axis, double mean){
        double maxDeviation = 0;

        for(int i=0; i < frameSamples; i++)
            maxDeviation = Math.max(Math.abs(samples[i][axis]-mean), maxDeviation);

        return maxDeviation;
    }

    private double calculateMaxDeviation(double[][] samples, int frameSamples, int axis){
        double mean = calculateMean(samples, frameSamples, axis);
        return calculateStandardDeviation(samples, frameSamples, axis, mean);
    }

    private double[] calculatePSDAcrossFrequencyBands(double[][] samples, int frameSamples, int axis, double mean){

        double accum;
        double[] psdAcrossFrequencyBands = new double[freqBandEdges.length -1];

        Arrays.fill(fftBufferI, 0);
        Arrays.fill(fftBufferR, 0);

        for(int i=0; i < frameSamples; i++)
            fftBufferR[i] = samples[i][axis] - mean;

        //In-place Windowing
        featureWin.applyWindow(fftBufferR);

        //In-place FFT
        featureFFT.fft(fftBufferR, fftBufferI);

        for(int b=0; b < (freqBandEdges.length-1); b++){
            int j = freqBandIdx[b];
            int k = freqBandIdx[b+1];

            accum=0;
            for(int h=j; h < k; h++)
                accum += fftBufferR[h]*fftBufferR[h] + fftBufferI[h]*fftBufferI[h];

            psdAcrossFrequencyBands[b] = accum / ((double) k-j);
        }

        return psdAcrossFrequencyBands;
    }

    private JsonObject getFeatures(double[][] samples, int frameSamples, int axis){
        JsonObject data = new JsonObject();
        Gson gson = getGson();

        double mean = calculateMean(samples, frameSamples, axis);
        data.addProperty(MEAN, mean);

        double absCentralMoment = calculateAbsoluteCentralMoment(samples, frameSamples, axis, mean);
        data.addProperty(ABSOLUTE_CENTRAL_MOMENT, absCentralMoment);

        double standardDev = calculateStandardDeviation(samples, frameSamples, axis, mean);
        data.addProperty(STANDARD_DEVIATION, standardDev);

        double maxDev = calculateMaxDeviation(samples, frameSamples, axis, mean);
        data.addProperty(MAX_DEVIATION, maxDev);

        double[] psd = calculatePSDAcrossFrequencyBands(samples, frameSamples, axis, mean);
        data.add(PSD_ACROSS_FREQUENCY_BANDS, gson.toJsonTree(psd));

        return data;
    }

    @Override
    public void execute(PipelineContext pipelineContext) {

        SensorPipeLineContext context = (SensorPipeLineContext) pipelineContext;
        AccelerometerSampleWindow window = context.getInput();

        JsonObject output = new JsonObject();

        featureFFT = new FFT(fftSize);
        featureWin = new Window(window.getTotalSamples());
        fftBufferI = new double[fftSize];
        fftBufferR = new double[fftSize];
        freqBandIdx = new int[freqBandEdges.length];

        for (int i = 0; i < freqBandEdges.length; i ++) {
            freqBandIdx[i] = Math.round((float)freqBandEdges[i]*((float)fftSize/(float) SensingUtils.ACCELEROMETER_MAX_RATE));
        }

        output.addProperty(SENSOR, SENSOR_TYPE);
        output.addProperty(SensingUtils.TIMESTAMP, window.getLastTimestamp());
        //TODO: add diff seconds?
        output.addProperty(SensingUtils.NUM_FRAME_SAMPLES, window.getTotalSamples());


        double[][] samples = window.getSamples();
        int numSamples = window.getTotalSamples();
        output.add(SensingUtils.X, getFeatures(samples, numSamples, 0));
        output.add(SensingUtils.Y, getFeatures(samples, numSamples, 1));
        output.add(SensingUtils.Z, getFeatures(samples, numSamples, 2));

        output.add("Samples", getGson().toJsonTree(samples));

        context.setOutput(output);
    }
}
