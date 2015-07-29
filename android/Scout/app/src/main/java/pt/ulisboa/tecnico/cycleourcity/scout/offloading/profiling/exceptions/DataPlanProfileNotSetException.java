package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.exceptions;

/**
 * Created by rodrigo.jm.lourenco on 28/07/2015.
 */
public class DataPlanProfileNotSetException extends Throwable {
    private String message = "Data plan information has yet to be defined.";

    @Override
    public String getMessage() {
        return message;
    }
}
