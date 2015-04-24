package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state;

import com.google.gson.JsonObject;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.Iterator;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.NoSuchDataFieldException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.data.Location;
import pt.ulisboa.tecnico.cycleourcity.scout.parser.SensingUtils;

/**
 * @version 1.0 Location-Only
 * @author rodrigo.jm.lourenco
 */
public class LocationState {

    public final static int     MIN_FIXED_SATELLITES = 3;
    public final static float   MAX_SPEED = (float) 12.5; // 12.5m = 45Km/h.
    public final static float   MIN_ACCURACY = (float) 40.0;//Error margin of +/- 40m.
    public final static int     ALTITUDE_VARIANCE_THRESHOLD = 15; //m

    public final static int     POISONED_BUFFER_THRESHOLD = 30; //s

    public final static int     LAST_LOCATIONS_SIZE = 5;

    private double  latitude, longitude;
    private float   altitude, slope, speed;

    private float pressureAltitude;

    private CircularFifoQueue<Location> lastLocations = new CircularFifoQueue<>(LAST_LOCATIONS_SIZE);

    synchronized public double getLatitude() {
        return latitude;
    }


    synchronized public double getLongitude() {
        return longitude;
    }


    synchronized public float getAltitude() { return altitude; }

    /**
     * Returns the average altitude given the last registered altitudes.
     * @return mean altitude
     */
    synchronized public float getAverageAltitude(){

        float avgAlt = 0;
        int size = lastLocations.size();
        Iterator<Location> iterator = lastLocations.iterator();

        while (iterator.hasNext()){
            avgAlt += iterator.next().getAltitude();
        }

        return size == LAST_LOCATIONS_SIZE ? avgAlt/size : 0;

    }

    synchronized public float getAverageSpeed(){

        float speedTotal=0;
        int size = lastLocations.size();
        Iterator<Location> iterator = lastLocations.iterator();

        while (iterator.hasNext())
            speedTotal += iterator.next().getSpeed();

        return size == LAST_LOCATIONS_SIZE ? speedTotal/size : 0;

    }

    synchronized public float getSlope() {
        return slope;
    }

    synchronized public void updateLocationState(JsonObject location){

        this.latitude = SensingUtils.LocationSampleAccessor.getLatitude(location);
        this.longitude = SensingUtils.LocationSampleAccessor.getLongitude(location);

        try {
            this.altitude = SensingUtils.LocationSampleAccessor.getAltitude(location);
        } catch (NoSuchDataFieldException e) {
            this.altitude = getAverageAltitude();
        }

        try {
            this.speed = SensingUtils.LocationSampleAccessor.getSpeed(location);
        } catch (NoSuchDataFieldException e) {
            this.speed = getAverageSpeed();
        }

        lastLocations.add(new Location(location));
    }

    synchronized public Location getLastLocation(){
        return this.lastLocations.peek();
    }

    synchronized public void clearLocationBuffer(){
        this.lastLocations.clear();
    }

    synchronized public boolean isReadyState(){
        return lastLocations.size() >= LAST_LOCATIONS_SIZE;
    }

    public float getPressureAltitude() {
        return pressureAltitude;
    }

    public void setPressureAltitude(float pressureAltitude) {
        this.pressureAltitude = pressureAltitude;
    }
}
