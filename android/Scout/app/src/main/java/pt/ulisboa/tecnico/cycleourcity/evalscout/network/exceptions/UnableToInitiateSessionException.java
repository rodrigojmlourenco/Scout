package pt.ulisboa.tecnico.cycleourcity.evalscout.network.exceptions;

/**
 * Created by rodrigo.jm.lourenco on 27/07/2015.
 */
public class UnableToInitiateSessionException extends Exception {
    private final String message = "Unable to acquire a session identifier.";

    @Override
    public String getMessage() {
        return message;
    }
}
