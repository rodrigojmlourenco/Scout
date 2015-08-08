package pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.exceptions;

import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.stages.ConfigurationTaggingStage;

/**
 * Created by rodrigo.jm.lourenco on 01/06/2015.
 */
public class TaggingStageMissingException extends AdaptiveOffloadingException {

    private final String message;

    public TaggingStageMissingException(){
        message = "Adaptive Offloading requires a "+
                ConfigurationTaggingStage.class.getSimpleName() +
                " to be added as a final stage.";
    }

    @Override
    public String getMessage() {
        return message;
    }
}
