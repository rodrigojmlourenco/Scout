package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.data;

import android.util.Log;

import edu.mit.media.funf.json.IJsonObject;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;

/**
 * Created by rodrigo.jm.lourenco on 13/03/2015.
 */
public class AccelerometerSample {

    private final static String LOG_TAG = "AccelerometerSample";

    private final int sensorType = SensingUtils.ACCELEROMETER;
    private int accuracy;
    private double timestamp;
    private double[] triAxialValues;
    private IJsonObject jsonSample;

    public AccelerometerSample(IJsonObject data){

        accuracy = data.get(SensingUtils.ACCURACY).getAsInt();
        timestamp= data.get(SensingUtils.TIMESTAMP).getAsDouble();
        triAxialValues = new double[]{  data.get(SensingUtils.X).getAsDouble(),
                data.get(SensingUtils.Y).getAsDouble(),
                data.get(SensingUtils.Z).getAsDouble()};
    }

    public AccelerometerSample(IJsonObject sensorConfig, IJsonObject data){

        if(SensingUtils.isAccelerometerSample(sensorConfig)) {
            accuracy = data.get(SensingUtils.ACCURACY).getAsInt();
            timestamp= data.get(SensingUtils.TIMESTAMP).getAsDouble();
            triAxialValues = new double[]{  data.get(SensingUtils.X).getAsDouble(),
                                            data.get(SensingUtils.Y).getAsDouble(),
                                            data.get(SensingUtils.Z).getAsDouble()};

        }else{
            //TODO: throw new exception
            Log.e(LOG_TAG, "Criar excepcoes para estes casos");
        }
    }

    public double getTimestamp(){
        return this.timestamp;
    }

    public int getAccuracy(){
        return this.accuracy;
    }

    public double getX(){
        return this.triAxialValues[0];
    }

    public double getY(){
        return this.triAxialValues[1];
    }

    public double getZ(){
        return this.triAxialValues[2];
    }

    public IJsonObject getSampleAsJSON(){
        return this.jsonSample;
    }
}
