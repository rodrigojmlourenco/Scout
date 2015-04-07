package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.data;

import java.util.ArrayList;

import edu.mit.media.funf.json.IJsonObject;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;

/**
 * Created by rodrigo.jm.lourenco on 13/03/2015.
 */
public class AccelerometerSampleWindow{

    /**
     * Defines the minimum size in seconds of an accelerometer sampling window. */
    public final static int SAMPLE_WINDOW_SIZE = 10; //seconds

    private double prevTime;
    private boolean isComplete;
    private double windowElapsedTime;
    private ArrayList<AccelerometerSample> accelerometerSamples;

    public AccelerometerSampleWindow(){
        this.accelerometerSamples = new ArrayList<>();
        this.isComplete = false;
    }

    public void pushAccelerometerSample(AccelerometerSample sample){
        if(!isComplete){
            double currTime = sample.getTimestamp();

            if(prevTime == 0){
                prevTime = currTime;
            }

            windowElapsedTime = currTime - prevTime;

            if(windowElapsedTime >= SAMPLE_WINDOW_SIZE)
                isComplete = true;
            else
                accelerometerSamples.add(sample);
        }
    }

    public void pushAccelerometerSample(IJsonObject config, IJsonObject data){

        if(!isComplete && SensingUtils.isAccelerometerSample(config)){

            AccelerometerSample sample = new AccelerometerSample(data);
            double currTime = sample.getTimestamp();

            if(prevTime == 0){
                prevTime = currTime;
            }

            windowElapsedTime = currTime - prevTime;

            if(windowElapsedTime >= SAMPLE_WINDOW_SIZE)
                isComplete = true;
            else
                accelerometerSamples.add(sample);
        }
    }

    public boolean isComplete(){
        return this.isComplete;
    }

    public int getTotalSamples(){
        return this.accelerometerSamples.size();
    }

    public double getWindowElapsedTime(){
        return windowElapsedTime;
    }

    /**
     * Retrieves the Accelerometer sampling window's samples as a double matrix
     * @return Accelerometer samples
     */
    public double[][] getSamples(){

        int numSamples = getTotalSamples();
        double[][] frameBuffer = new double[numSamples][3];

        for(int i=0; i < numSamples; i++){
            AccelerometerSample sample = accelerometerSamples.get(i);

            frameBuffer[i][0] = sample.getX();
            frameBuffer[i][1] = sample.getY();
            frameBuffer[i][2] = sample.getZ();
        }

        return frameBuffer;
    }

    public double getLastTimestamp(){
        return accelerometerSamples.get(accelerometerSamples.size()-1).getTimestamp();
    }
}
