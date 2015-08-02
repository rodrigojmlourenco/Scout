package pt.ulisboa.tecnico.cycleourcity.scout.calibration.exceptions;

public class NotYetCalibratedException extends Exception{

    String message = "Scout has not yet been calibrated.";

    @Override
    public String getMessage() {
        return message;
    }
}
