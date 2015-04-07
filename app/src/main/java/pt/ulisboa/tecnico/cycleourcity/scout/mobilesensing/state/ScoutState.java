package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state;

import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;

/**
 * @version 1.0 Location-Only
 * @author rodrigo.jm.lourenco
 *
 * This class represents the Scout Application's internal state regarding the user's location and
 * movement.
 */
public class ScoutState {

    private static String LOG_TAG = "ScoutState";

    private double timestamp;

    //Singleton
    private static ScoutState SCOUT_STATE = new ScoutState();

    //Logging
    private ScoutLogger logger = ScoutLogger.getInstance();

    //Internal States
    private MotionState motionState;
    private LocationState locationState;

    private ScoutState(){
        this.motionState = new MotionState();
        this.locationState = new LocationState();
    }

    public static ScoutState getInstance(){
        return SCOUT_STATE;
    }

    public MotionState getMotionState() {
        return motionState;
    }

    public LocationState getLocationState() {
        return locationState;
    }

    synchronized public double getTimestamp() {
        return timestamp;
    }

    synchronized public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }
}
