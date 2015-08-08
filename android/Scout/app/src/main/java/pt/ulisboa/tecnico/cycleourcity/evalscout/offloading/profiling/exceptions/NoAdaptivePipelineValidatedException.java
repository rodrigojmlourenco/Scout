package pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.profiling.exceptions;

import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.exceptions.AdaptiveOffloadingException;

/**
 * Created by rodrigo.jm.lourenco on 05/06/2015.
 */
public class NoAdaptivePipelineValidatedException extends AdaptiveOffloadingException {
    private String message;

    public NoAdaptivePipelineValidatedException(){
        message = "Unable to perform adaptive offloading. "+
                    "No AdaptivePipelines have been validated and registed in the PipelinePartitionEngine.";
    }

    @Override
    public String getMessage() {
        return message;
    }
}
