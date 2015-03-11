package pt.ulisboa.tecnico.cycleourcity.scout.sensing.config;

import com.ubhave.sensormanager.config.SensorConfig;
import com.ubhave.sensormanager.config.pull.ApplicationConfig;
import com.ubhave.sensormanager.config.pull.BluetoothConfig;
import com.ubhave.sensormanager.config.pull.CameraConfig;
import com.ubhave.sensormanager.config.pull.ContentReaderConfig;
import com.ubhave.sensormanager.config.pull.LocationConfig;
import com.ubhave.sensormanager.config.pull.MicrophoneConfig;
import com.ubhave.sensormanager.config.pull.MotionSensorConfig;
import com.ubhave.sensormanager.config.pull.PhoneRadioConfig;
import com.ubhave.sensormanager.config.pull.PullSensorConfig;
import com.ubhave.sensormanager.config.pull.WifiConfig;
import com.ubhave.sensormanager.config.push.PassiveLocationConfig;
import com.ubhave.sensormanager.sensors.SensorUtils;

import pt.ulisboa.tecnico.cycleourcity.scout.sensing.config.pull.ScoutLocationConfig;

/**
 * Created by rodrigo.jm.lourenco on 11/03/2015.
 */
public class ScoutSensorConfig extends SensorConfig implements Cloneable {

    public static SensorConfig getDefaultConfig(int sensorType)
    {
        SensorConfig sensorConfig = new SensorConfig();
        switch (sensorType)
        {
            case SensorUtils.SENSOR_TYPE_ACCELEROMETER:
            case SensorUtils.SENSOR_TYPE_GYROSCOPE:
            case SensorUtils.SENSOR_TYPE_MAGNETIC_FIELD:
                sensorConfig = MotionSensorConfig.getDefault();
                break;
            case SensorUtils.SENSOR_TYPE_BLUETOOTH:
                sensorConfig = BluetoothConfig.getDefault();
                break;
            case SensorUtils.SENSOR_TYPE_LOCATION: //OVERWRITTEN
                sensorConfig = ScoutLocationConfig.getDefault();
                break;
            case SensorUtils.SENSOR_TYPE_MICROPHONE:
                sensorConfig = MicrophoneConfig.getDefault();
                break;
            case SensorUtils.SENSOR_TYPE_WIFI:
                sensorConfig = WifiConfig.getDefault();
                break;
            case SensorUtils.SENSOR_TYPE_APPLICATION:
                sensorConfig = ApplicationConfig.getDefault();
                break;
            case SensorUtils.SENSOR_TYPE_SMS_CONTENT_READER:
            case SensorUtils.SENSOR_TYPE_CALL_CONTENT_READER:
                sensorConfig = ContentReaderConfig.getDefault();
                break;
            case SensorUtils.SENSOR_TYPE_CAMERA:
                sensorConfig = CameraConfig.getDefault();
                break;
            case SensorUtils.SENSOR_TYPE_PHONE_RADIO:
                sensorConfig = PhoneRadioConfig.getDefault();
                break;
            case SensorUtils.SENSOR_TYPE_PASSIVE_LOCATION:
                sensorConfig = PassiveLocationConfig.getDefault();
                break;
        }
        sensorConfig.setParameter(PullSensorConfig.ADAPTIVE_SENSING_ENABLED, false);
        return sensorConfig;
    }
}
