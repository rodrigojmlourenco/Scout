package pt.ulisboa.tecnico.cycleourcity.scout.network.exceptions;

/**
 * Created by rodrigo.jm.lourenco on 27/07/2015.
 */
public class SampleUploadFailedException extends Exception {
    private String message = "Upload failed with error code ";

    public SampleUploadFailedException(String errorCode){
        this.message += errorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
