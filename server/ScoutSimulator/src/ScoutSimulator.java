import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Scanner;

import com.google.gson.JsonObject;

import simulator.SensingSimulator;
import simulator.exceptions.SimulatorAlreadyInitializedException;
import simulator.exceptions.UnableToInitializeException;



public class ScoutSimulator {

	public static final String SAMPLE_BASE_DIR 	= "./extern/scoutSamples";
	public static final String DIVIDER			= "----------------------------------------\n";
	public static final String BASE_COMMAND_OPTIONS =
			DIVIDER +
			"[HOME] Please select a command \n"+
			"'l' or 'list' \t- Lists the existing samples files\n"+
			"'s' or 'select'\t- Select sample file for simulation\n"+
			"'q' or 'quit' \t- Terminates the simulation\n"+
			"> ";

	public static final String SIMULATION_COMMAND_OPTIONS =
			DIVIDER +
			"[SIMULATION] Please select a command \n"+
			"'s' or 'start'\t- Start a new sensing session\n"+
			"'h' or 'home'\t- Goes back to the home menu\n"+
			"'q' or 'quit' \t- Terminates the simulation\n"+
			"> ";
	
	public static final String SENSING_COMMAND_OPTIONS =
			DIVIDER +
			"[SIMULATION] Please select a command \n"+
			"'s' or 'start'\t- Start a new sensing session\n"+
			"'e' or 'end'\t- End the sensing session\n"+
			"'h' or 'home'\t- Goes back to the home menu\n"+
			"'q' or 'quit' \t- Terminates the simulation\n"+
			"> ";

	public static Scanner INPUT_READER = new Scanner(System.in);

	public static void listExistingSampleFiles(){
		File basedir = new File(SAMPLE_BASE_DIR);

		System.out.print(DIVIDER);
		System.out.println("Files in "+SAMPLE_BASE_DIR+":");

		for(String file : basedir.list())
			System.out.println("- "+file);
	}

	public static File selectSimuationFile() throws NoSuchFileException{

		System.out.print(DIVIDER);
		System.out.print("Select a sample file : ");

		String sampleFilename = INPUT_READER.nextLine();
		File sampleFile = new File(SAMPLE_BASE_DIR, sampleFilename);

		if(!sampleFile.exists())
			throw new NoSuchFileException(sampleFile.getAbsolutePath());

		return sampleFile;

	}

	public static void cloneFile(){

	}

	public static void main(String[] args) {

		SensingSimulator simulator = new SensingSimulator();
		System.out.println("Welcome to the Scout simulation");

		File sampleFile = null;

		boolean continueSimulation =true;

		String command;
		do{

			if(sampleFile == null){
				System.out.print(BASE_COMMAND_OPTIONS);
				command = INPUT_READER.nextLine();

				switch (command.toLowerCase()) {
				case "l":
				case "list":
					listExistingSampleFiles();
					break;
				case "s":
				case "select":
					try {
						sampleFile = selectSimuationFile();
						simulator.destroy();
						simulator.initialize(sampleFile);
					} catch (NoSuchFileException e) {
						System.err.println("The select file does not exists, use 'l' to list existing files.");
					} catch (SimulatorAlreadyInitializedException e) {
						System.err.println("The simulator has already been initialized.");
						sampleFile = null;
					} catch (UnableToInitializeException e) {
						System.err.println("Unable to initialize the simulator.");
						sampleFile = null;
					}
					break;
				case "q":
				case "quit":
					System.out.println("Exiting the simulation...");
					INPUT_READER.close();
					return;
				default:
					System.out.println("Command not supported");
					break;
				}
			}else{
				System.out.print(SIMULATION_COMMAND_OPTIONS);
				command = INPUT_READER.nextLine();
				switch (command.toLowerCase()) {
				case "b":
				case "back":
					sampleFile = null;
					break;
				case "q":
				case "quit":
					System.out.println("Exiting the simulation...");
					INPUT_READER.close();
					return;
				default:
					System.out.println("Command not supported");
					break;
				}	


			}


		}while(continueSimulation);


	}
}
