package pt.ulisboa.tecnico.cycleourcity.scout.learning;

/**
 * Created by rodrigo.jm.lourenco on 15/07/2015.
 */
public class PavementType {

    public static enum Pavements {
        undefined,
        asphalt,
        gravel,
        cobblestone
    }

    private Object lock = new Object();
    private Pavements pavementType;

    private PavementType(){};

    private static PavementType INSTANCE = new PavementType();

    public static PavementType getInstance(){ return  INSTANCE; }

    public void setPavementType(Pavements pavement){
        synchronized (lock){
            pavementType = pavement;
        }
    }

    public String getPavementType(){
        synchronized (lock){
            switch (pavementType){
                case asphalt:
                    return "asphalt";
                case cobblestone:
                    return "cobblestone";
                case gravel:
                    return "gravel";
                default:
                    return "undefined";
            }
        }
    }
}
