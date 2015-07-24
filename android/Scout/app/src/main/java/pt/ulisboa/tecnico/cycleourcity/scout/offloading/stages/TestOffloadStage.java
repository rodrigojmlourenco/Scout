package pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages;

import android.util.Log;

import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;

/**
 * Created by rodrigo.jm.lourenco on 09/07/2015.
 */
public class TestOffloadStage implements Stage {

    protected final String LOG_TAG = "TestStage";
    private final int wait;

    public TestOffloadStage(int wait){
        this.wait = wait;
    }


    @Override
    public void execute(PipelineContext pipelineContext) {

        for(int i=0; i < this.wait; i++ );

        Log.d(LOG_TAG, "waited for "+wait);
    }
}
