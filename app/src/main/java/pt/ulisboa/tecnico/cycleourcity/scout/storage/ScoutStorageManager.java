package pt.ulisboa.tecnico.cycleourcity.scout.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import com.google.gson.JsonObject;

import java.io.File;
import java.sql.SQLException;

import edu.mit.media.funf.storage.DefaultArchive;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import pt.ulisboa.tecnico.cycleourcity.scout.ScoutApplication;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;

/**
 * Created by rodrigo.jm.lourenco on 30/03/2015.
 */
public class ScoutStorageManager implements StorageManager{

    private static ScoutStorageManager INSTANCE = new ScoutStorageManager();
    private final static String NAME = "Scout";
    private final static String LOG_TAG = "ScoutStorageManager";
    private final static int DB_VERSION = 1;

    private NameValueDatabaseHelper dbHelper;
    private DefaultArchive archive;

    private ScoutStorageManager(){
        Context ctx = ScoutApplication.getContext();
        archive = new DefaultArchive(ctx, NAME);
        dbHelper = new NameValueDatabaseHelper(ctx, NAME, DB_VERSION);


        File file = new File(Environment.getExternalStorageDirectory(), NAME);
        if (!file.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
        }else
            Log.d(LOG_TAG, file.getAbsolutePath());

    }

    public static ScoutStorageManager getInstance(){
        return INSTANCE;
    }

    @Override
    public void store(String key, JsonObject values) throws SQLException {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        final double timestamp = values.get(SensingUtils.TIMESTAMP).getAsDouble();
        String value = values.toString();

        if (timestamp == 0L || key == null || value == null) {
            Log.e(LOG_TAG, "Unable to save data.  Not all required values specified. " + timestamp + " " + key + " - " + value);
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
        if (archive.add(dbFile)) {
            dbFile.delete();
        }
        dbHelper.getWritableDatabase(); // Build new database
        Log.d(LOG_TAG, "archived!");
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
