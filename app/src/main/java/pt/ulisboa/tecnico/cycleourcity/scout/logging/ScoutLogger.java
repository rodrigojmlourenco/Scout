package pt.ulisboa.tecnico.cycleourcity.scout.logging;

import android.util.Log;

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
                    Log.v(tag, message);
                    break;
                case DEBUG:
                    Log.d(tag, message);
                    break;
                case INFO:
                    Log.i(tag, message);
                    break;
                case WARN:
                    Log.w(tag, message);
                    break;
                case ERR:
                    Log.e(tag, message);
                    break;
            }
    }
}
