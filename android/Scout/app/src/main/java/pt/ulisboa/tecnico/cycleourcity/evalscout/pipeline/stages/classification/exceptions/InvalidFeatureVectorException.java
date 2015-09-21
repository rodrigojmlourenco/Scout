package pt.ulisboa.tecnico.cycleourcity.evalscout.pipeline.stages.classification.exceptions;

/**
 * Created by rodrigo.jm.lourenco on 24/08/2015.
 */
public class InvalidFeatureVectorException extends Throwable {
    private String message;

    public InvalidFeatureVectorException(String error){
        message = error;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
