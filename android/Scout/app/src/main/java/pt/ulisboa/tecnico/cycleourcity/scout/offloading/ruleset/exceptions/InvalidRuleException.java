package pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.exceptions;

/**
 * Created by rodrigo.jm.lourenco on 29/07/2015.
 */
public class InvalidRuleException extends Throwable{

    private final String message;

    public InvalidRuleException(String message){
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
