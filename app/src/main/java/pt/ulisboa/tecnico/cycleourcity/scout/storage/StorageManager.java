package pt.ulisboa.tecnico.cycleourcity.scout.storage;

import com.google.gson.JsonObject;

import java.sql.SQLException;

/**
 * Created by rodrigo.jm.lourenco on 30/03/2015.
 */
public interface StorageManager {

    public final static String LOG_TAG = "Storage";

    public void store(String key, JsonObject value) throws SQLException;

    public void archive();

    public void archive(String tag);
}
