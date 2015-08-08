package pt.ulisboa.tecnico.cycleourcity.evalscout.calibration.exceptions;

/**
 * Created by rodrigo.jm.lourenco on 20/07/2015.
 */
public class UninitializedException extends Exception {
    private final String message = "ScoutCalibrationManager must first be initiated before used.";

    @Override
    public String getMessage() {
        return message;
    }
}
