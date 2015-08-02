package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.exceptions;

/**
 * Created by rodrigo.jm.lourenco on 02/08/2015.
 */
public class InvalidMobilePlanStateException extends Exception {

    private final Exception topException;

    public InvalidMobilePlanStateException(Exception e){
        topException = e;
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }

    @Override
    public void printStackTrace() {
        topException.printStackTrace();
    }
}
