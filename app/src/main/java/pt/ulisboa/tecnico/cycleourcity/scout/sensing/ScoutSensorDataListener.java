package pt.ulisboa.tecnico.cycleourcity.scout.sensing;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.ubhave.dataformatter.DataFormatter;
import com.ubhave.dataformatter.json.JSONFormatter;

import com.ubhave.datahandler.except.DataHandlerException;
import com.ubhave.sensormanager.ESException;
import com.ubhave.sensormanager.ESSensorManager;
import com.ubhave.sensormanager.SensorDataListener;
import com.ubhave.sensormanager.config.GlobalConfig;
import com.ubhave.sensormanager.config.pull.LocationConfig;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.sensormanager.data.pull.LocationData;
import com.ubhave.sensormanager.sensors.SensorUtils;

import pt.ulisboa.tecnico.cycleourcity.scout.backend.ApplicationContext;

/**
 * Created by rodrigo.jm.lourenco on 11/03/2015.
 */
public class ScoutSensorDataListener implements SensorDataListener {



    private final static String LOG_TAG = "SensorListener";

    private final int sensorType;

    private ESSensorManager sensorManager;
    private final JSONFormatter formatter;

    private int sensorSubscriptionId;
    private boolean isSubscribed;

    public ScoutSensorDataListener(int sensorType) throws ESException {

        Context context = ApplicationContext.getContext();

        this.sensorType = sensorType;
        this.sensorManager = ESSensorManager.getSensorManager(context);
        this.formatter = DataFormatter.getJSONFormatter(context, sensorType);

        this.isSubscribed = false;

        if (sensorType == SensorUtils.SENSOR_TYPE_LOCATION)
            sensorManager.setSensorConfig(SensorUtils.SENSOR_TYPE_LOCATION, LocationConfig.ACCURACY_TYPE, LocationConfig.LOCATION_ACCURACY_FINE);

    }


    public void subscribeToSensorData() throws ESException{
        this.sensorSubscriptionId = sensorManager.subscribeToSensorData(sensorType, this);
        this.isSubscribed = true;
    }

    public void unsubscribeFromSensorData() throws ESException{
        sensorManager.unsubscribeFromSensorData(this.sensorSubscriptionId);
        this.isSubscribed=false;
    }

    public void pauseSensorData() throws ESException {
        sensorManager.pauseSubscription(this.sensorSubscriptionId);
    }

    public void resumeSensorData() throws ESException {
        sensorManager.unPauseSubscription(this.sensorSubscriptionId);
    }

    @Override
    public void onDataSensed(SensorData sensorData) {
        //TODO: do something with the data, for now log it


        try {
            String jsonValue = formatter.toJSON(sensorData).toString();
            Log.d(LOG_TAG, "[Sensor:" + SensorUtils.getSensorName(sensorType) + "] - " + jsonValue);
        } catch (ESException e) {
            e.printStackTrace();
        } catch (DataHandlerException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onCrossingLowBatteryThreshold(boolean bellowThreshold) {
        //TODO: do something with the data, for now just warn me

        if(bellowThreshold)
            Log.w(LOG_TAG, "Battery is getting low");
        else
            Log.w(LOG_TAG, "Battery is healthy again");

    }
}
