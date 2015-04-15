package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state;

import com.google.gson.JsonObject;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.NoSuchDataFieldException;
import pt.ulisboa.tecnico.cycleourcity.scout.parser.SensingUtils;

/**
 * @version 1.0 Location-Only
 * @author rodrigo.jm.lourenco
 */
public class LocationState {

    public final static int MIN_FIXED_SATELLITES = 3;
    public final static float MAX_SPEED = (float) 12.5; // 12.5m = 45Km/h.
    public final static float MIN_ACCURACY = (float) 40.0;//Error margin of +/- 40m.

    public final static int LAST_LOCATIONS_SIZE = 3;

    private double  latitude, longitude;
    private float   altitude, slope, speed;

    private CircularFifoQueue<JsonObject> lastKnownLocations = new CircularFifoQueue<>(LAST_LOCATIONS_SIZE);

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

    synchronized public void updateLocationState(JsonObject location){

        this.latitude = SensingUtils.LocationSampleAccessor.getLatitude(location);
        this.longitude = SensingUtils.LocationSampleAccessor.getLongitude(location);

        try {
            this.altitude = SensingUtils.LocationSampleAccessor.getAltitude(location);
        } catch (NoSuchDataFieldException e) {
            e.printStackTrace();
        }

        try {
            this.speed = SensingUtils.LocationSampleAccessor.getSpeed(location);
        } catch (NoSuchDataFieldException e) {
            e.printStackTrace();
        }

        lastKnownLocations.add(location);
    }


    synchronized public float getSpeed() {
        return speed;
    }
}
