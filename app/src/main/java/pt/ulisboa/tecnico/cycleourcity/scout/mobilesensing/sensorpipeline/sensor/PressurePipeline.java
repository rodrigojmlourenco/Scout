package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.sensor;

import android.hardware.SensorManager;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.util.LinkedList;
import java.util.Queue;

import edu.mit.media.funf.json.IJsonObject;
import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeLineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.LocationState;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.ScoutState;

/**
 * Created by rodrigo.jm.lourenco on 24/04/2015.
 */
public class PressurePipeline implements ISensorPipeline {

    private static final String TAG = "[PRESSURE]";
    private static final String LOG_TAG = "PressurePipeline";

    private static final SensorPipeline PRESSURE_PIPELINE = new SensorPipeline();

    //Debugging
    private ScoutLogger logger = ScoutLogger.getInstance();

    private Queue<JsonObject> samplesQueue;


    static{
        PRESSURE_PIPELINE.addStage(new TestStage());
        PRESSURE_PIPELINE.addStage(new UpdateScoutStateStage());
    }

    public PressurePipeline(){
        this.samplesQueue = new LinkedList<>();
    }



    @Override
    public void pushSample(IJsonObject sensorSample) {
        this.samplesQueue.add(sensorSample.getAsJsonObject());
    }

    @Override
    public void pushSample(JsonObject sensorSample) {
        this.samplesQueue.add(sensorSample);

    }

    @Override
    public void run() {

        JsonObject[] input;

        //Merge the two queues, clear them, and pass the result as input
        synchronized (this) {
            input = new JsonObject[samplesQueue.size()];
            samplesQueue.toArray(input);
            samplesQueue.clear();
        }

        logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG+"executing pipeline for "+input.length+" motion samples.");

        SensorPipeLineContext context = new SensorPipeLineContext();
        context.setInput(input);
        PRESSURE_PIPELINE.execute(context);
    }

    /****************************************************************************************
     * STAGES: Stages to be used by the Pressure Pipeline                                   *
     ****************************************************************************************/
    public static class TestStage implements Stage {

        private final String TAG = "[Altitude]";
        private ScoutLogger logger = ScoutLogger.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {

            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();
            Queue<JsonObject> parsedInput = new LinkedList<>();

            float altitude;
            float atmosphericPressure;
            for(JsonObject sample : input){

                if(sample.has("pressure")){
                    atmosphericPressure = sample.get("pressure").getAsFloat();
                    altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, atmosphericPressure);

                    logger.log(ScoutLogger.DEBUG, TAG, String.valueOf(altitude));

                    sample.addProperty("altitude", altitude);
                    parsedInput.add(sample);


                }else{
                    logger.log(ScoutLogger.ERR, PressurePipeline.TAG, String.valueOf(sample));
                }

            }

            JsonObject[] newInput = new JsonObject[parsedInput.size()];
            parsedInput.toArray(newInput);

            ((SensorPipeLineContext)pipelineContext).setInput(newInput);

        }
    }

    public static class UpdateScoutStateStage implements Stage {

         private LocationState state = ScoutState.getInstance().getLocationState();

        @Override
        public void execute(PipelineContext pipelineContext) {

            JsonObject[] input = ((SensorPipeLineContext)pipelineContext).getInput();

            state.setPressureAltitude(input[0].get("altitude").getAsFloat());

        }
    }
}
