package pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.exceptions;

/**
 * Created by rodrigo.jm.lourenco on 02/06/2015.
 */
public class ProfilerIsAlreadyRunningException extends AdaptiveOffloadingException{
    private String message;

    public ProfilerIsAlreadyRunningException(){
        this.message = "Unable to change profiling rate, as the profiler is already running.";
    }

    @Override
    public String getMessage() {
        return message;
    }
}
