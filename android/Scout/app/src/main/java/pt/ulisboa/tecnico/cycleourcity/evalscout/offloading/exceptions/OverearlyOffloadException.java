package pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.exceptions;

/**
 * Created by rodrigo.jm.lourenco on 24/07/2015.
 */
public class OverearlyOffloadException extends Exception {
    private final String message =
            "It is too early to perform offload, as the stages have not yet been profiled.";

    @Override
    public String getMessage() {
        return message;
    }
}
