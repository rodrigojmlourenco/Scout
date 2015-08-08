package pt.ulisboa.tecnico.cycleourcity.evalscout.offloading;

import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import pt.ulisboa.tecnico.cycleourcity.evalscout.storage.ScoutStorageManager;

/**
 * Created by rodrigo.jm.lourenco on 03/06/2015.
 */
public class OffloadingLogger {

    //TESTING //TODO: remover
    private static boolean VERBOSE = true;

    //Logging
    private Queue<String> logMessages;
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    DateFormat fileDateFormat = new SimpleDateFormat("dd:MM:yyyy_HHmmss");

    //Storage
    private ScoutStorageManager storage;
    public final static String BASEDIR = "offloading";

    private OffloadingLogger(){
        this.logMessages = new LinkedList<>();
        storage = ScoutStorageManager.getInstance();
    }

    private static OffloadingLogger LOGGER = new OffloadingLogger();

    protected static void log(String component, String log){
        String message = LOGGER.generateLog(component, log);
        LOGGER.logMessages.add(message);
        if(VERBOSE) Log.d(AdaptiveOffloadingManager.LOG_TAG, message);
    }

    protected static void exportLog(){

        if(LOGGER.logMessages.isEmpty()) return;

        String filename = "log"+LOGGER.fileDateFormat.format(new Date())+".log";
        LOGGER.storage.archiveLog(BASEDIR, filename, LOGGER.logMessages);
        LOGGER.logMessages.clear();
    }

    private String generateLog(String component, String message){
        Date date = new Date();
        return "["+component+" - " + dateFormat.format(date)+"]: "+ message;
    }
}

