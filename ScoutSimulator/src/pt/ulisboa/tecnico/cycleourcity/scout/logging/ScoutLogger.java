package pt.ulisboa.tecnico.cycleourcity.scout.logging;

import java.util.Calendar;

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

    
    private String log(String tag, String message){
    	Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		return (calendar.getTime().toGMTString()+"["+tag+"]:\t"+message);
    	
    }
    
    public void log(int level, String tag, String message) {

        if(this.level <= level )
            switch (level){
                case VERBOSE:
                    System.out.println(log(tag, message));
                    break;
                case DEBUG:
                    System.out.println(log(tag, message));
                    break;
                case INFO:
                    System.out.println(log(tag, message));
                    break;
                case WARN:
                    System.out.println(log(tag, message));
                    break;
                case ERR:
                    System.out.println(log(tag, message));
                    break;
            }
    }
}
