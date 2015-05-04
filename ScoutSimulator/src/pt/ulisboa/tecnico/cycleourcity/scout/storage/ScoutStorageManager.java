package pt.ulisboa.tecnico.cycleourcity.scout.storage;

import java.sql.SQLException;

import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.exceptions.NothingToArchiveException;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.gpx.GPXBuilder;

import com.google.gson.JsonObject;

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

	private boolean empty = true;

	@Override
	public void clearStoredData() {
		// TODO Auto-generated method stub

	}

	@Override
	public void store(String key, JsonObject value) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void archive() throws NothingToArchiveException {
		// TODO Auto-generated method stub

	}

	@Override
	public void archive(String tag) throws NothingToArchiveException {
		// TODO Auto-generated method stub

	}

	@Override
	public void archiveGPXTrack(String tag) {

		GPXBuilder builder = GPXBuilder.getInstance();

		builder.storeAllGPXTracks("./extern/tracks", tag);

	}

	public static ScoutStorageManager getInstance() {
		return INSTANCE;
	}

}
