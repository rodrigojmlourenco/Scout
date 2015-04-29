package pt.ulisboa.tecnico.cycleourcity.scout.logging;

//import android.util.Log;

/**
 * Created by rodrigo.jm.lourenco on 03/04/2015.
 */
public class ScoutLogger {

    private static ScoutLogger LOGGER = new ScoutLogger();

    public static final int VERBOSE = 1,
                            DEBUG   = 2,
                            INFO    = 3,
                            WARN    = 4,
                            ERR     = 5;

    private boolean log;
    private int level;

    private ScoutLogger(){
        this.log = true;
        this.level = 0;
    }

    public static ScoutLogger getInstance(){
        return LOGGER;
    }

    public void disableLogger(){ this.log = false; }

    public void enableLogger(){ this.log = true; }

    public void setLoggingLevel(int level){ this.level = level; }

    public void log(int level, String tag, String message) {

        if(this.level <= level )
            switch (level){
                case VERBOSE:
                    System.out.println(".v(tag, message);"); //TODO
                    break;
                case DEBUG:
                    System.out.println("Log.d(tag, message);"); //TODO
                    break;
                case INFO:
                    System.out.println("Log.i(tag, message)"); //TODO
                    break;
                case WARN:
                    System.out.println("Log.w(tag, message);"); //TODO
                    break;
                case ERR:
                    System.out.println("Log.e(tag, message)"); //TODO
                    break;
            }
    }
}
