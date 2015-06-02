package pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions;

import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.AdaptiveOffloadingTaggingStage;

/**
 * Created by rodrigo.jm.lourenco on 01/06/2015.
 */
public class TaggingStageMissingException extends AdaptiveOffloadingException {

    private final String message;

    public TaggingStageMissingException(){
        message = "Adaptive Offloading requires a "+
                AdaptiveOffloadingTaggingStage.class.getSimpleName() +
                " to be added as a final stage.";
    }

    @Override
    public String getMessage() {
        return message;
    }
}
