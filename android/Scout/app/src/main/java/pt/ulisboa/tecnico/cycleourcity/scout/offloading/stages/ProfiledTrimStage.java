package pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages;

import android.util.Log;

import com.ideaimpl.patterns.pipeline.PipelineContext;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.location.LocationSensorPipeline;

/**
 * Created by rodrigo.jm.lourenco on 29/05/2015.
 */
public class ProfiledTrimStage extends LocationSensorPipeline.TrimStage {

    @Override
    public void execute(PipelineContext pipelineContext) {

        long t1 = System.nanoTime();

        super.execute(pipelineContext);

        Log.d("TrimStage", "TrimStage took "+(System.nanoTime()-t1)+"ns");
    }

}
