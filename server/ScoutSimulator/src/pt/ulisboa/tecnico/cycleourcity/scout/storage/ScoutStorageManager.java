package pt.ulisboa.tecnico.cycleourcity.scout.storage;

import java.sql.SQLException;

import pt.ulisboa.tecnico.cycleourcity.scout.storage.exceptions.NothingToArchiveException;

import com.google.gson.JsonObject;

/**
 * Created by rodrigo.jm.lourenco on 30/03/2015.
 */
public class ScoutStorageManager implements StorageManager{

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
		// TODO Auto-generated method stub
		
	}

	public static ScoutStorageManager getInstance() {
		// TODO Auto-generated method stub
		return null;
	}

}
