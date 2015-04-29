package simulator;

import java.io.File;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import sensing.sqlite.SQLiteLoader;
import simulator.exceptions.SimulatorAlreadyInitializedException;
import simulator.exceptions.UnableToInitializeException;
import simulator.funf.SimulatedPipeline;

import com.google.gson.JsonObject;

public class SensingSimulator {
	
	public final static String LOG_TAG = "[Simulator] - ";
	
	//Internal State
	public static final int 
	STATE_INIT 		= 0,
	STATE_IDLE		= 1,
	STATE_SENSING 	= 2,
	STATE_EMPTY		= 3;
	private int state = STATE_INIT;
	
	
	
	public Queue<JsonObject> sampleList;
	
	public SensingSimulator(){
		sampleList = new LinkedList<JsonObject>(); 
	}
	
	private SimulatedPipeline funfPipeline = new SimulatedPipeline();
	
	
	private void log(String message){
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		System.out.println(calendar.getTime().toGMTString()+"\t- "+message);
	}
	
	public void initialize(File samplesDB) 
			throws SimulatorAlreadyInitializedException, UnableToInitializeException{
		
		if(state > STATE_INIT)
			throw new SimulatorAlreadyInitializedException();
		
		try {
			SQLiteLoader samplesLoader = new SQLiteLoader(samplesDB);
			sampleList = new LinkedList<JsonObject>(samplesLoader.fetchTableValues());
			samplesLoader.close();
			
			this.state++;
		
			log(sampleList.size()+" samples loaded");
		
			
		} catch (ClassNotFoundException e) {
			throw new UnableToInitializeException();
		} catch (SQLException e) {
			throw new UnableToInitializeException();
		}
	}
	
	public void destroy(){
		state = STATE_INIT;
		
		try{
			funfPipeline.stopSensing();
			sampleList.clear();
		}catch(NullPointerException e){
			e.printStackTrace();
		}
	}
	
	public void startSensingSession(){
		
		funfPipeline.startSensing();
		state++;
		
	}
	
	public void stopSensingSession() {
		
		funfPipeline.stopSensing();
		state--;
		
	}

}
