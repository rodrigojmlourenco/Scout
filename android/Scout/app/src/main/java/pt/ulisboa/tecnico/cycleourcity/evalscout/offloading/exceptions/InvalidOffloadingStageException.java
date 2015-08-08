package pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.exceptions;

import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.stages.OffloadingWrapperStage;

/**
 * Created by rodrigo.jm.lourenco on 01/06/2015.
 */
public class InvalidOffloadingStageException extends AdaptiveOffloadingException {
    private final String message;

    public InvalidOffloadingStageException(){
        this.message = "All stages in a PipelineConfiguration must be wrapped by a "
                + OffloadingWrapperStage.class.getSimpleName()+".";
    }

    @Override
    public String getMessage() {
        return message;
    }
}
