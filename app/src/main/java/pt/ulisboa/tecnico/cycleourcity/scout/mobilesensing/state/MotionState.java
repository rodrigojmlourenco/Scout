package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state;

/**
 * @version 1.0 Location-Only
 * @author rodrigo.jm.lourenco
 */
public class MotionState {

    private double speed;
    private String travelState;

    synchronized public double getSpeed() {
        return speed;
    }

    synchronized public void setSpeed(double speed) {
        this.speed = speed;
    }

    synchronized public String getTravelState() {
        return travelState;
    }

    synchronized public void setTravelState(String travelState) {
        this.travelState = travelState;
    }
}
