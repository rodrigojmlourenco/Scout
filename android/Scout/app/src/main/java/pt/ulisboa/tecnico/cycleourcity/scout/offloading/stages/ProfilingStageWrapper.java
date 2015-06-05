package pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.UUID;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.PipelinePartitionEngine;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.StageProfiler;

/**
 * Created by rodrigo.jm.lourenco on 31/05/2015.
 */
public class ProfilingStageWrapper implements Stage {

    private final Stage stage;
    private final UUID identifier;
    private Boolean profilingEnabled = true;
    private CircularFifoQueue<Long> executionTimes;
    private CircularFifoQueue<DataProfileInfo> dataSizes;
    private StageProfiler profiler = StageProfiler.getInstance();

    public ProfilingStageWrapper(Stage stage){
        this.stage = stage;
        identifier = profiler.registerStage(this);
        dataSizes = new CircularFifoQueue<>(StageProfiler.NUM_PROFILING_SAMPLES);
        executionTimes = new CircularFifoQueue<>(StageProfiler.NUM_PROFILING_SAMPLES);
    }

    public Class<? extends Stage> getStageClass(){
        return stage.getClass();
    }

    public void toggleProfiling(){
        profilingEnabled = false;
    }

    public UUID getStageUUID(){
        return this.identifier;
    }

    @Override
    public void execute(PipelineContext pipelineContext) {

        Gson gson = new Gson();
        long startTime = 0, endTime;
        long initDataSize=0, endDataSize, totalData=0;

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
    }

    public long getAverageRunningTime(){

        long total = 0;

        for(Long time : executionTimes)
            total += time;

        return total / executionTimes.size();
    }

    public long getAverageGeneratedDataSize(){
        long total = 0;

        for(DataProfileInfo info : dataSizes)
            total += info.getGeneratedDataSize();

        return total;
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

        private long initalDataSize;
        private long finalDataSize;

        public DataProfileInfo(long initalDataSize,long finalDataSize){
            this.initalDataSize = initalDataSize;
            this.finalDataSize = finalDataSize;
        }

        public long getSizeDifference(){
            return  initalDataSize - finalDataSize;
        }

        public float getCompressionRate(){
            return  ((float)finalDataSize)/initalDataSize;
        }

        public long getGeneratedDataSize(){ return this.finalDataSize; }
    }
}
