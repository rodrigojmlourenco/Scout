package pt.ulisboa.tecnico.cycleourcity.scout.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import edu.mit.media.funf.storage.DefaultArchive;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import pt.ulisboa.tecnico.cycleourcity.scout.ScoutApplication;
import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.archive.ScoutArchive;

/**
 * Created by rodrigo.jm.lourenco on 30/03/2015.
 */
public class ScoutStorageManager implements StorageManager{

    private static ScoutStorageManager INSTANCE = new ScoutStorageManager();
    private final static String NAME = "Scout";
    private final static String LOG_TAG = "ScoutStorageManager";
    private final static int DB_VERSION = 1;

    //Logging
    private ScoutLogger logger = ScoutLogger.getInstance();

    private NameValueDatabaseHelper dbHelper;
    //private DefaultArchive archive;
    private ScoutArchive archive;

    private ScoutStorageManager(){
        Context ctx = ScoutApplication.getContext();
        //archive = new DefaultArchive(ctx, NAME);
        archive = new ScoutArchive(ctx, NAME);
        dbHelper = new NameValueDatabaseHelper(ctx, NAME, DB_VERSION);
    }

    public static ScoutStorageManager getInstance(){
        return INSTANCE;
    }

    private File getApplicationFolder(){
        File dir = new File(Environment.getExternalStorageDirectory(), NAME);

        //Debugging
        if (!dir.mkdirs()) {
            logger.log(ScoutLogger.INFO, LOG_TAG, "Directory '"+dir.getAbsolutePath()+"'not created");
        }else
            logger.log(ScoutLogger.INFO, LOG_TAG, "Directory '"+dir.getAbsolutePath()+"' created");

        return dir;
    }

    @Override
    public void clearStoredData() {
        dbHelper.getWritableDatabase().delete("data", null, null);
    }

    @Override
    public void store(String key, JsonObject values) throws SQLException {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        final double timestamp = values.get(SensingUtils.TIMESTAMP).getAsDouble();
        String value = values.toString();

        if (timestamp == 0L || key == null || value == null) {
            logger.log(ScoutLogger.ERR, LOG_TAG, "Unable to save data.  Not all required values specified. " + timestamp + " " + key + " - " + value);
            throw new SQLException("Not all required fields specified.");
        }

        ContentValues cv = new ContentValues();
        cv.put(NameValueDatabaseHelper.COLUMN_NAME, key);
        cv.put(NameValueDatabaseHelper.COLUMN_VALUE, value);
        cv.put(NameValueDatabaseHelper.COLUMN_TIMESTAMP, timestamp);
        db.insertOrThrow(NameValueDatabaseHelper.DATA_TABLE.name, "", cv);
    }

    @Override
    public void archive() {

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        File dbFile = new File(db.getPath());
        db.close();

        logger.log(ScoutLogger.DEBUG, LOG_TAG, "dbFile created at '"+dbFile.getAbsolutePath()+"'.");

        if (archive.add(dbFile, "something")){
            dbFile.delete();
            try {
                logger.log(ScoutLogger.DEBUG, LOG_TAG, archive.getPathOnSDCard());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        dbHelper.getWritableDatabase(); // Build new database
        logger.log(ScoutLogger.INFO, LOG_TAG, "Samples were successfully archived");
        //setHandler(null); // free system resources
    }

    @Override
    public void archive(String tag) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        File dbFile = new File(db.getPath());
        db.close();

        logger.log(ScoutLogger.DEBUG, LOG_TAG, "dbFile created at '"+dbFile.getAbsolutePath()+"'.");

        if (archive.add(dbFile, tag)){
            dbFile.delete();
            try {
                logger.log(ScoutLogger.DEBUG, LOG_TAG, archive.getPathOnSDCard());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        dbHelper.getWritableDatabase(); // Build new database
        logger.log(ScoutLogger.INFO, LOG_TAG, "Samples were successfully archived");
        //setHandler(null); // free system resources
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
}
