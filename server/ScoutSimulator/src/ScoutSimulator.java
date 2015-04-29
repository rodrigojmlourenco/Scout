import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exceptions.MobileSensingException;

import com.google.gson.JsonObject;

import sensing.sqlite.SQLiteLoader;
import simulator.SensingSimulator;



public class ScoutSimulator {
	
	public static final String SAMPLE_BASE_DIR = "./extern/scoutSamples";

	public static void main(String[] args) {
		
		SensingSimulator simulator = new SensingSimulator();
		
		String sampleFilename = "";
		File sampleFile = null;
		Scanner scanner = new Scanner(System.in);
		
		System.out.print("> ");
		
		if(scanner.hasNext()){
			
			sampleFilename = scanner.nextLine();
			sampleFile = new File(SAMPLE_BASE_DIR, sampleFilename);
			
			if(!sampleFile.exists()){
				System.err.println("Sample file '"+sampleFilename+"' not found.");
				return;
			}else{
				System.out.println("Loading file '"+sampleFile.getAbsolutePath()+"'...");
			}
		}
		
		List<JsonObject> samples = null;
		
		try {
			SQLiteLoader loader = new SQLiteLoader(sampleFile);
			samples = loader.fetchTableValues();
			loader.close();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		scanner.close();
	}
}
