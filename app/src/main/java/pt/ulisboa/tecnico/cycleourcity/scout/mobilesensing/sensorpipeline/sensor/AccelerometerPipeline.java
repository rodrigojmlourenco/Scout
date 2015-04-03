package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor;

import android.util.Log;

import com.google.gson.JsonObject;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import edu.mit.media.funf.json.IJsonObject;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.stages.FeatureExtractionStage;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.data.AccelerometerSample;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.data.AccelerometerSampleWindow;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeLineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeline;

/**
 * Created by rodrigo.jm.lourenco on 13/03/2015.
 */
public class AccelerometerPipeline implements ISensorPipeline{

    private static final SensorPipeline PIPELINE = new SensorPipeline();

    static{
        PIPELINE.addFinalStage(new FeatureExtractionStage());
    }

    //Queues for further processing
    private Queue<AccelerometerSample> sampleList;
    private Queue<AccelerometerSampleWindow> windowList;

    //Tasks
    private Timer timer;
    private AggregateSamplesTask aggregateTask;

    public AccelerometerPipeline(){

        //Queues initialization
        sampleList = new LinkedList<>();
        windowList = new LinkedList<>();

        timer = new Timer(true);
        aggregateTask = new AggregateSamplesTask();
        timer.scheduleAtFixedRate(aggregateTask, 0, SensingUtils.ACCELEROMETER_WINDOW_SIZE*1000);
        aggregateTask.run();
    }


    /**
     *
     */
    public void stopPipeline(){
        aggregateTask.cancel();
    }

    @Override
    public void pushSample(IJsonObject sensorSample) {
        sampleList.add(new AccelerometerSample(sensorSample));
    }

    /* Asynchronous task, that every ACCELEROMETER_WINDOW_SIZE attempts to create a new accelerometer
     * sampling window, adding the newly created sample window to the window queue. */
    private class AggregateSamplesTask extends TimerTask{
        @Override
        public void run() {
            synchronized (sampleList){

                if(sampleList.size() >= SensingUtils.ACCELEROMETER_WINDOW_SIZE){

                    AccelerometerSampleWindow window = new AccelerometerSampleWindow();

                    while (!window.isComplete() && sampleList.size() > 0)
                        window.pushAccelerometerSample(sampleList.remove());

                    windowList.add(window);

                    //Pipeline Testing
                    SensorPipeLineContext context = new SensorPipeLineContext();
                    context.setInput(window);
                    PIPELINE.execute(context);

                    JsonObject output = context.getOutput();
                    Log.d("TASK", String.valueOf(output));
                }
            }
        }
    }
}
