import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Scanner;

import javax.swing.JFrame;

import org.bson.Document;

import com.google.gson.JsonObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import simulator.SensingSimulator;
import simulator.exceptions.SimulatorAlreadyInitializedException;
import simulator.exceptions.UnableToInitializeException;
import simulator.funf.FunfPipeline;



public class ScoutSimulator {

	public static final String BASE_DIR 	= "./extern/";
	public static final String SAMPLE_BASE_DIR 	= "./extern/scoutSamples";
	
	

	
	
	
	public static Scanner INPUT_READER = new Scanner(System.in);

	

	public static void cloneFile(){

	}

	public static void main(String[] args) {

		SensingSimulator simulator = new SensingSimulator();
		System.out.println("Welcome to the Scout simulation");

		MongoClient mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase database = mongoClient.getDatabase("sensing");
		MongoCollection<Document> collection = database.getCollection("samples");
		
		Document doc = new Document("name", "MongoDB")
	        .append("type", "database")
	        .append("count", 1)
	        .append("info", new Document("x", 203).append("y", 102));
		
		collection.insertOne(doc);
		mongoClient.close();
		
		
		File sampleFile = null;

		boolean continueSimulation =true;
		
		ScoutSimulatorGUI gui = new ScoutSimulatorGUI();
		gui.setTitle("CycleOurCity Scout - Simulator");
		gui.setSize(600, 400);
		gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		gui.setVisible(true);
		

		CommandManager cMan = new CommandManager();
		cMan.executeAction();


	}
	
	private static interface ScoutCommand {
		
		public final String DIVIDER			= "----------------------------------------\n";
		
		
		public void printOptions();
		
		public int execute(String command);
		
	}
	
	public static class HomeCommand implements ScoutCommand {
		
		public final String BASE_COMMAND_OPTIONS =
				DIVIDER +
				"[HOME] Please select a command \n"+
				"'l' or 'list' \t- Lists the existing samples files\n"+
				"'s' or 'select'\t- Select sample file for simulation\n"+
				"'q' or 'quit' \t- Terminates the simulation\n"+
				"> ";
		
		public void listExistingSampleFiles(){
			File basedir = new File(SAMPLE_BASE_DIR);

			System.out.print(DIVIDER);
			System.out.println("Files in "+SAMPLE_BASE_DIR+":");

			for(String file : basedir.list())
				System.out.println("- "+file);
		}

		public File selectSimuationFile() throws NoSuchFileException{

			System.out.print(DIVIDER);
			System.out.print("Select a sample file : ");

			String sampleFilename = INPUT_READER.nextLine();
			File sampleFile = new File(SAMPLE_BASE_DIR, sampleFilename);

			if(!sampleFile.exists())
				throw new NoSuchFileException(sampleFile.getAbsolutePath());

			return sampleFile;

		}


		private File sampleFile;
		
		@Override
		public int execute(String command) {
			
			switch (command.toLowerCase()) {
			case "l":
			case "list":
				listExistingSampleFiles();
				return 0;
			case "s":
			case "select":
				try {
					CommandManager.sampleFile = selectSimuationFile();
				} catch (NoSuchFileException e) {
					System.err.println("The select file does not exists, use 'l' to list existing files.");
				}
				return 1;
			case "q":
			case "quit":
				System.out.println("Exiting the simulation...");
				INPUT_READER.close();
				System.exit(0);
			default:
				System.out.println("Command not supported");
				return 0;
			}
			
		}

		
		@Override
		public void printOptions() {
			System.out.print(BASE_COMMAND_OPTIONS);
		}
	}
	
	public static class SimulationMenuCommand implements ScoutCommand {
		
		public File sampleFile;
		
		public final String SIMULATION_COMMAND_OPTIONS =
				DIVIDER +
				"[SIMULATION] Please select a command \n"+
				"'s' or 'start'\t- Start a new sensing session\n"+
				"'h' or 'home'\t- Goes back to the home menu\n"+
				"'q' or 'quit' \t- Terminates the simulation\n"+
				"> ";


		@Override
		public void printOptions() {
			System.out.print(SIMULATION_COMMAND_OPTIONS);
			
		}

		@Override
		public int execute(String command) {
			switch (command.toLowerCase()) {
			case "s":
			case "start":
				try {
					CommandManager.simulator.initialize(CommandManager.sampleFile);
				} catch (SimulatorAlreadyInitializedException
						| UnableToInitializeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return 1;
			case "h":
			case "home":
				CommandManager.sampleFile = null;
				return -1;
			case "q":
			case "quit":
				System.out.println("Exiting the simulation...");
				INPUT_READER.close();
				System.exit(0);
			default:
				System.out.println("Command not supported");
				return 0;
			}	
		}
	}
	
	public static class SensingSessionMenu implements ScoutCommand {

		
		private final String SENSING_COMMAND_OPTIONS =
				DIVIDER +
				"[SENSING] Please select a command \n"+
				"'s' or 'start'\t- Start a new sensing session\n"+
				"'r' or 'run'\t- Run all the samples\n"+
				"'stop'\t- Stop the mobile sensing pipeline\n"+
				"'store'\t- Store the processed sampling session\n"+
				"'q' or 'quit' \t- Terminates the simulation\n"+
				"> ";

		
		@Override
		public void printOptions() {
			System.out.print(SENSING_COMMAND_OPTIONS);
			
		}

		@Override
		public int execute(String command) {
			
			String tag;
			
			switch (command.toLowerCase()) {
			case "s":
			case "start":
				CommandManager.simulator.startSensingSession();
				return 0;
			case "r":
			case "run":
				CommandManager.simulator.runAllSamples();
				return 0;
			case "stop":
				System.out.println("Stopping the MobileSensingPipeline...");
				CommandManager.simulator.stopSensingSession();
				return 0;
			case "store":
				System.out.print("Tag name: ");
				tag = INPUT_READER.nextLine();
				CommandManager.simulator.setTag(tag);
				CommandManager.simulator.runAction(FunfPipeline.ACTION_ARCHIVE);
				return -1;
			case "q":
			case "quit":
				System.out.println("Exiting the simulation...");
				INPUT_READER.close();
				System.exit(0);
			default:
				System.out.println("Command not supported");
				return 0;
			}
		}
		
	}
	
	
	public static class CommandManager {
		
		String command;
		Scanner scanner = new Scanner(System.in);
		ScoutCommand commandExecutor = new HomeCommand();
		
		public static File sampleFile;
		public static SensingSimulator simulator = new SensingSimulator();
		
		private final int
				HOME 		= 0,
				SIMULATION 	= 1,
				SENSING		= 2;
		
		private int state = HOME;
	
		
		public void executeAction(){
			
			while(true){
				
				
				commandExecutor.printOptions();
				command = scanner.nextLine();
				
				state += commandExecutor.execute(command);
				
				switch (state) {
				case HOME:
					commandExecutor = new HomeCommand();
					break;
				case SIMULATION:
					commandExecutor = new SimulationMenuCommand();
					break;
				case SENSING:
					commandExecutor = new SensingSessionMenu();
					break;
				default:
					break;
				}
				
			}
		}
		
	}
}
