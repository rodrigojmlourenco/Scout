package pt.ulisboa.tecnico.cycleourcity.evalscout.learning;

/**
 * Created by rodrigo.jm.lourenco on 15/07/2015.
 */
public class PavementType {

    public enum Pavements {
        undefined,
        asphalt,
        gravel,
        cobblestone,
        //Pavement Types 2.0
        AsphaltGood,
        AsphaltBad,
        CobblestoneGood,
        CobblestoneBad,
        GravelGood,
        GravelBad
    }

    private Object lock = new Object();
    private Pavements pavementType = Pavements.undefined;

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
                case AsphaltGood:
                    return "AsphaltGood";
                case AsphaltBad:
                    return "AsphaltBad";
                case CobblestoneGood:
                    return  "CobblestoneGood";
                case CobblestoneBad:
                    return "CobblestoneBad";
                case GravelGood:
                    return "GravelGood";
                case GravelBad:
                    return "GravelBad";
                default:
                    return "undefined";
            }
        }
    }
}
