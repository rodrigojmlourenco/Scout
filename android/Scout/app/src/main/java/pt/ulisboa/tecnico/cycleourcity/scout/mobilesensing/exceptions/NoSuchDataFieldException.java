package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exceptions;

/**
 * Created by rodrigo.jm.lourenco on 07/04/2015.
 */
public class NoSuchDataFieldException extends Exception {

    private String dataField;

    public NoSuchDataFieldException(String dataField){
        super();
        this.dataField = dataField;
    }

    @Override
    public String getMessage() {
        return "'"+dataField+"' could not be found on the provided sample.";
    }
}
