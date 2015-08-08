package pt.ulisboa.tecnico.cycleourcity.evalscout.config;

/**
 * Created by rodrigo.jm.lourenco on 10/04/2015.
 */
public class ConfigurationFactory {

    //Static Configurations
    public static class PredefinedConfigurations {
        public final static String ONLY_LOCATION_CONFIG =
                "{\"@type\":\"ScoutPipeline\",\n" +
                        "\"name\":\"default\", " +
                        "\"version\":1, " +
                        "\"data\":[ " +
                        "{ " +
                        "\"@type\":\"edu.mit.media.funf.probe.builtin.LocationProbe\", " +
                        "\"@schedule\": { \"interval\":5, \"strict\":true}, " +
                        //"\"@schedule\": { \"interval\":5, \"duration\":1, \"strict\":true}, " +
                        "\"sensorDelay\": \"NORMAL\"" +
                        "} " +
                        "] " +
                        "}";

        public final static String ONLY_ACCELEROMETER_CONFIG =
                "{\"@type\":\"ScoutPipeline\",\n" +
                        "\"name\":\"default\", " +
                        "\"version\":1, " +
                        "\"data\":[ " +
                        "{ " +
                        "\"@type\":\"edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe\", " +
                        "\"@schedule\": { \"interval\":5, \"duration\":0, \"strict\":true}, " +
                        //"\"@schedule\": { \"interval\":5, \"duration\":1, \"strict\":true}, " +
                        "\"sensorDelay\": \"NORMAL\"" +
                        "} " +
                        "] " +
                        "}";
    }
}
