package pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.math3.stat.StatUtils;

import java.util.Arrays;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.pipelines.StageProfiler;

/**
 * This class is used to wrap a Stage as to enable Stage profiling.
 * <br>
 * In order for offloading to be possible all adaptive Stages must be wrapped by this class.
 */
public class OffloadingWrapperStage implements Stage {

    protected final Stage stage;
    protected final String identifier;
    protected Boolean profilingEnabled = true;
    protected CircularFifoQueue<Long> executionTimes;
    protected CircularFifoQueue<DataProfileInfo> dataSizes;
    protected StageProfiler profiler = StageProfiler.getInstance();

    public OffloadingWrapperStage(String identifier, Stage stage){

        this.stage = stage;
        this.identifier = identifier;

        if(!profiler.hasBeenModeled(identifier)){ //Profiling is necessary

            dataSizes = new CircularFifoQueue<>(StageProfiler.NUM_PROFILING_SAMPLES);
            executionTimes = new CircularFifoQueue<>(StageProfiler.NUM_PROFILING_SAMPLES);

            if(profiler.hasModel())
                Log.e(StageProfiler.LOG_TAG, "Unexpected case: has model but stage "+identifier+" hasn't been modelled.");

            profiler.registerStage(identifier, this);

        }else{ //Profiling is not necessary

            dataSizes = null;
            executionTimes = null;

            if(StageProfiler.VERBOSE)
                Log.d(StageProfiler.LOG_TAG,
                        identifier+" has already been modelled, and so profiling will be disabled");

            profilingEnabled = false;
        }
    }



    @Override
    public void execute(PipelineContext pipelineContext) {

        Gson gson = new Gson();
        long startTime = 0, endTime;
        long initDataSize=0, endDataSize, totalData;

        if(profilingEnabled) {
            //Time Profiling
            startTime = System.nanoTime();

            //Data Profiling
            JsonObject[] input = ((SensorPipelineContext)pipelineContext).getInput();
            totalData = 0;
            for(JsonObject sample : input)
                totalData += sampleMemSize(gson.toJson(sample));
            initDataSize = totalData;
        }

        this.stage.execute(pipelineContext);

        if(profilingEnabled) {
            //Time Profiling
            endTime = System.nanoTime();
            executionTimes.add(endTime - startTime);

            //Data Profiling
            JsonObject[] output = ((SensorPipelineContext)pipelineContext).getInput();
            totalData = 0;
            for(JsonObject sample : output)
                totalData += sampleMemSize(gson.toJson(sample));
            endDataSize = totalData;
            dataSizes.add(new DataProfileInfo(initDataSize, endDataSize));
        }

        if(profilingEnabled)
            if(!profiler.hasBeenModeled(identifier) && isInitialMonitoringComplete()) {

                if(StageProfiler.VERBOSE)
                    Log.d(StageProfiler.LOG_TAG, "Generating a model for the stage "+identifier+".");

                profiler.generateStageModel(
                        identifier,
                        stage.getClass().getCanonicalName(),
                        getAverageRunningTime(),
                        getAverageInputDataSize(),
                        getAverageGeneratedDataSize());

                this.profilingEnabled = false;

            }else {
                if(StageProfiler.VERBOSE)
                    Log.d(StageProfiler.LOG_TAG, "["+identifier+"]:"
                            +(StageProfiler.NUM_PROFILING_SAMPLES-executionTimes.size())+" iterations to go.");
            }
    }

    protected boolean isInitialMonitoringComplete(){
        return (StageProfiler.NUM_PROFILING_SAMPLES -executionTimes.size() == 0)
                && (StageProfiler.NUM_PROFILING_SAMPLES -dataSizes.size() == 0);
    }

    @Deprecated
    public Class<? extends Stage> getStageClass(){
        return stage.getClass();
    }

    public double getMedianRunningTime(){

        double median;
        Long[] times = new Long[StageProfiler.NUM_PROFILING_SAMPLES];
        executionTimes.toArray(times);

        Arrays.sort(times);

        if (times.length % 2 == 0)
            median = ((double)times[StageProfiler.NUM_PROFILING_SAMPLES/2] + (double)times[StageProfiler.NUM_PROFILING_SAMPLES/2 - 1])/2;
        else
            median = (double) times[StageProfiler.NUM_PROFILING_SAMPLES/2];

        return median;
    }

    @Deprecated
    public long getAverageRunningTime(){


        long total = 0;

        for(Long time : executionTimes)
            total += time;

        return total / executionTimes.size();
    }

    public double getMedianOutputtedDataSize(){
        double median;
        Long[] data = new Long[StageProfiler.NUM_PROFILING_SAMPLES];

        for(int i=0; i < StageProfiler.NUM_PROFILING_SAMPLES; i++)
            data[i] = dataSizes.get(i).getGeneratedDataSize();


        Arrays.sort(data);

        if (data.length % 2 == 0)
            median = ((double)data[StageProfiler.NUM_PROFILING_SAMPLES/2] + (double)data[StageProfiler.NUM_PROFILING_SAMPLES/2 - 1])/2;
        else
            median = (double) data[StageProfiler.NUM_PROFILING_SAMPLES/2];

        return median;
    }

    public double getMedianInputtedDataSize(){
        double median;
        Long[] data = new Long[StageProfiler.NUM_PROFILING_SAMPLES];

        for(int i=0; i < StageProfiler.NUM_PROFILING_SAMPLES; i++)
            data[i] = dataSizes.get(i).getInputDataSize();


        Arrays.sort(data);

        if (data.length % 2 == 0)
            median = ((double)data[StageProfiler.NUM_PROFILING_SAMPLES/2] + (double)data[StageProfiler.NUM_PROFILING_SAMPLES/2 - 1])/2;
        else
            median = (double) data[StageProfiler.NUM_PROFILING_SAMPLES/2];

        return median;
    }

    @Deprecated
    public long getAverageGeneratedDataSize(){
        long total = 0;

        for(DataProfileInfo info : dataSizes)
            total += info.getGeneratedDataSize();

        return total / dataSizes.size();
    }


    @Deprecated
    public long getAverageInputDataSize(){
        long total = 0;

        for(DataProfileInfo info : dataSizes)
            total += info.getInputDataSize();

        return total / dataSizes.size();
    }

    private long sampleMemSize(String sample){
        return sample.length()*2;
    }


    /**
     * Generates relevant stage information, as a JSON string.
     * @return Stage information
     */
    public String dumpInfo(){
        return "{ stage: \""+stage.getClass().getName()+"\", "+
                "duration: "+getAverageRunningTime() +", "+
                "data: "+getAverageGeneratedDataSize()+
                "}";
    }

    public static class DataProfileInfo {

        private long initialDataSize;
        private long finalDataSize;

        public DataProfileInfo(long initialDataSize,long finalDataSize){
            this.initialDataSize = initialDataSize;
            this.finalDataSize = finalDataSize;
        }

        public long getGeneratedDataSize(){ return this.finalDataSize; }

        public long getInputDataSize() { return  this.initialDataSize; }
    }

    public String getIdentifier(){
        return identifier;
    }
}
