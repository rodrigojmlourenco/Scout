package pt.ulisboa.tecnico.cycleourcity.scout.sensing.config.pull;

import com.ubhave.sensormanager.config.SensorConfig;
import com.ubhave.sensormanager.config.pull.PullSensorConfig;

/**
 * TODO: further investigation must be performed in order to determine the optimal configuration
 *
 * Created by rodrigo.jm.lourenco on 11/03/2015.
 *
 * Scout's location sensor is to be configured as specified in the
 * <a href="http://www.cs.cornell.edu/~destrin/resources/conferences/2010apr-Reddy-Shilton-Biketastic.pdf">
 *     Biketastic application
 * </a>
 * <br>
 * That is:
 * <ul>
 *     <li>Fine-Grained Location Accuracy</li>
 *     <li>Capture information every one second</li>
 * </ul>
 * <br>
 *
 * <h3>Side-note</h3>
 * Although the sampling window size is set to 1s, it is important to mention that if the WiFi is turned
 * on this sampling window will not work, as the fail ration is too high.
 * <br>
 * With the WiFi turned-on the fail ratio seems to drop substantially for sampling rates over 3.5~4s.
 */
public class ScoutLocationConfig{

    public final static String ACCURACY_TYPE = "LOCATION_ACCURACY";
    public final static String LOCATION_ACCURACY_COARSE = "LOCATION_ACCURACY_COARSE";
    public final static String LOCATION_ACCURACY_FINE = "LOCATION_ACCURACY_FINE";

    public static final long DEFAULT_SAMPLING_WINDOW_SIZE_MILLIS = 1000;
    public static final long DEFAULT_SLEEP_INTERVAL = 1000;
    public final static int LOCATION_CHANGE_DISTANCE_THRESHOLD = 100;



    public static SensorConfig getDefault()
    {
        SensorConfig sensorConfig = new SensorConfig();
        sensorConfig.setParameter(PullSensorConfig.POST_SENSE_SLEEP_LENGTH_MILLIS, DEFAULT_SLEEP_INTERVAL);
        sensorConfig.setParameter(PullSensorConfig.SENSE_WINDOW_LENGTH_MILLIS, DEFAULT_SAMPLING_WINDOW_SIZE_MILLIS);
        sensorConfig.setParameter(ACCURACY_TYPE, LOCATION_ACCURACY_FINE);

        return sensorConfig;
    }


}
