package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.exceptions;

/**
 * Created by rodrigo.jm.lourenco on 30/05/2015.
 */
public class UnknownNetworkTypeException extends Exception {

    private String message;

    public UnknownNetworkTypeException(String typeName) {
        this.message = "Unknown network type "+typeName;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
