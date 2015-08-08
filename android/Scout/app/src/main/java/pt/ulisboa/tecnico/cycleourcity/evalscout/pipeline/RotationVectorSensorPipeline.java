package pt.ulisboa.tecnico.cycleourcity.evalscout.pipeline;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.PipelineConfiguration;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.FeatureExtractor;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.SensorProcessingPipeline;

/**
 * Created by rodrigo.jm.lourenco on 25/06/2015.
 */
public class RotationVectorSensorPipeline extends SensorProcessingPipeline implements FeatureExtractor {

    //Logging
    private final String LOG_TAG = this.getClass().getSimpleName();

    public RotationVectorSensorPipeline(PipelineConfiguration configuration) {
        super(SensingUtils.Sensors.ROTATION_VECTOR, configuration);
    }
}
