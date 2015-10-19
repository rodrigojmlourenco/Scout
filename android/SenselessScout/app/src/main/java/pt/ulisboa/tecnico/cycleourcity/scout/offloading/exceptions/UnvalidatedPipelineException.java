package pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions;

/**
 * Created by rodrigo.jm.lourenco on 24/07/2015.
 */
public class UnvalidatedPipelineException extends Exception {

    private final String message = "This pipeline was not validated.";

    @Override
    public String getMessage() {
        return message;
    }
}
