package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exceptions;

/**
 * Created by rodrigo.jm.lourenco on 26/03/2015.
 */
public class SensorNotSupportedException extends MobileSensingException {
    private final String sensorName;

    public SensorNotSupportedException(String sensorName){
        super();
        this.sensorName = sensorName;
    }

    @Override
    public String getMessage() {
        return super.getMessage()+"\n"+sensorName+" is currently not supported by the application.";
    }
}
