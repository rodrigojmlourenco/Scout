package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.stages;

import android.util.Log;

import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

/**
 * Created by rodrigo.jm.lourenco on 13/03/2015.
 */
public abstract class AbstractFeatureExtractionStage implements Stage {

    private final static String LOG_TAG = "AbstractFeatureExtractionStage";
    @Override
    public void execute(PipelineContext pipelineContext) {
        Log.e(LOG_TAG, "NOT IMPLEMETED YET");
    }
}
