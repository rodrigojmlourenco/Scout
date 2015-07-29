package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.exceptions;

import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.AdaptiveOffloadingException;

/**
 * Created by rodrigo.jm.lourenco on 05/06/2015.
 */
public class NothingToOffloadException extends AdaptiveOffloadingException {

    private String message;

    public NothingToOffloadException(){
        message = "Skipping offloading attempt, as all adaptive stages have been offloaded.";
    }
}
