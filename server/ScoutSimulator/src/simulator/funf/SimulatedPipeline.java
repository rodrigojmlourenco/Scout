package simulator.funf;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.MobileSensingPipeline;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SimulatedPipeline implements FunfPipeline {
	
	private MobileSensingPipeline mPipeline = MobileSensingPipeline.getInstance();
	private boolean started = false;
	
	
	public void startSensing(){
		mPipeline.startSensingSession();
		started = true;
	}
	
	public void stopSensing(){
		mPipeline.stopSensingSession();
		started = false;
	}
	
	@Override
	public void onRun(String action, JsonElement config) {

			switch (action) {
			case ACTION_ARCHIVE:
				System.out.println("TODO: archive");
				break;
			case ACTION_UPDATE:
				System.out.println("TODO: update");
				break;
			case ACTION_UPLOAD:
				System.out.println("TODO: updload");
				break;
			default:
				break;
			}
		
	}

	@Override
	public void onDataReceived(JsonObject data) {
		if(started)
			mPipeline.pushSensorSample(data);
	}

	@Override
	public void onDataCompleted(JsonObject probeConfig, JsonObject checkpoint) {
		// TODO Auto-generated method stub
	}

}
