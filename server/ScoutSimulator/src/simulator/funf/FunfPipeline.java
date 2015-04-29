package simulator.funf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public interface FunfPipeline {
	
	public static final String 
		ACTION_ARCHIVE 	= "archive",
		ACTION_UPLOAD	= "upload",
		ACTION_UPDATE	= "update";
	
	/**
	 * Instructs the pipeline to perform an action.
	 * @param action
	 * @param config
	 */
	public void onRun(String action, JsonElement config);
	
	/**
	 * Called when the probe emits data.
	 * <br>
	 * Data emitted from probes that extend the Probe class are guaranteed to have the PROBE and TIMESTAMP parameters.
	 */
	public void onDataReceived(JsonObject data);
	
	/**
	 * Called when the probe is finished sending data.
	 * <br>
	 * This can be used to know when the probe was run, even if it didn't send data. 
	 * It can also be used to get a checkpoint of far through the data stream the probe ran. 
	 * Continuable probes can use this checkpoint to start the data stream where it previously left off.
	 */
	public void onDataCompleted(JsonObject probeConfig, JsonObject checkpoint);
	

}
