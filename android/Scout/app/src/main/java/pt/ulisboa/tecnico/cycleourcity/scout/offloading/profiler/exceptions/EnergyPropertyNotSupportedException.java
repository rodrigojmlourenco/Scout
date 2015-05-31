package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.exceptions;

/**
 * Created by rodrigo.jm.lourenco on 29/05/2015.
 */
public class EnergyPropertyNotSupportedException extends Exception {

    private String message;

    public EnergyPropertyNotSupportedException(String propertyName){
        this.message = "The device's BatteryManager is does not support "+propertyName+ " profiling.";
    }
}
