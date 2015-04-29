package simulator;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

import sensing.sqlite.SQLiteLoader;
import simulator.exceptions.SimulatorAlreadyInitializedException;
import simulator.exceptions.UnableToInitializeException;

import com.google.gson.JsonObject;

public class SensingSimulator {
	
	public final static String LOG_TAG = "[Simulator] - ";
	private int state;
	
	public List<JsonObject> sampleList;
	
	public static final int 
		STATE_INIT 		= 0,
		STATE_IDLE		= 1,
		STATE_SENSING 	= 2,
		STATE_EMPTY		= 3;
	
	public void initialize(File samplesDB) 
			throws SimulatorAlreadyInitializedException, UnableToInitializeException{
		
		if(state > STATE_INIT)
			throw new SimulatorAlreadyInitializedException();
		
		try {
			SQLiteLoader samplesLoader = new SQLiteLoader(samplesDB);
			sampleList = samplesLoader.fetchTableValues();
			samplesLoader.close();
			
			this.state = STATE_IDLE;
		
			System.out.println(LOG_TAG+sampleList.size()+" samples were loaded");
			
		} catch (ClassNotFoundException e) {
			throw new UnableToInitializeException();
		} catch (SQLException e) {
			throw new UnableToInitializeException();
		}
	}
	
	public void startSensingSession(){
		
	}
	
	public void stopSensingSession() {
		
	}

}
