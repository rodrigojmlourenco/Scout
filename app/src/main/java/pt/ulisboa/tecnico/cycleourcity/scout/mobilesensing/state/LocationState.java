package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state;

/**
 * @version 1.0 Location-Only
 * @author rodrigo.jm.lourenco
 */
public class LocationState {

    private double  latitude, longitude;
    private float   altitude, slope;


    synchronized public double getLatitude() {
        return latitude;
    }

    synchronized public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    synchronized public double getLongitude() {
        return longitude;
    }

    synchronized public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    synchronized public float getAltitude() {
        return altitude;
    }

    synchronized public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    synchronized public float getSlope() {
        return slope;
    }

    synchronized public void setSlope(float slope) {
        this.slope = slope;
    }
}
