package pt.ulisboa.tecnico.cycleourcity.evalscout.evaluation;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.PipelineConfiguration;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.SensorProcessingPipeline;

/**
 * Created by rodrigo.jm.lourenco on 10/08/2015.
 */
public class GenericSensorPipeline extends SensorProcessingPipeline {

    public GenericSensorPipeline(int sensorType, PipelineConfiguration configuration) {
        super(sensorType, configuration);
    }

    @Override
    public void run() {

        SensorPipelineContext context = new SensorPipelineContext();
        pipeline.execute(context);

       // super.run();
    }
}
