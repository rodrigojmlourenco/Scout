package simulator.funf;

import java.sql.SQLException;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.MobileSensingPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.exceptions.NothingToArchiveException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SimulatedPipeline implements FunfPipeline {
	
	private MobileSensingPipeline mPipeline = MobileSensingPipeline.getInstance();
	private boolean started = false;
	
	private String samplingTag;
	
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
				try {
					mPipeline.archiveData(samplingTag);
				} catch (SQLException | NothingToArchiveException e) {
					e.printStackTrace();
				}
				break;
			case ACTION_UPDATE:
				System.out.println("TODO: update");
				break;
			case ACTION_UPLOAD:
				System.out.println("TODO: upload");
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

	@Override
	public void setSamplingTag(String samplingTag) {
        this.samplingTag = samplingTag;
    }
}
