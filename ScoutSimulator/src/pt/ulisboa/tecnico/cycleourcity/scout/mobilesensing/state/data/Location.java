package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.data;

import com.google.gson.JsonObject;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exceptions.NoSuchDataFieldException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.location.LocationUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.LocationState;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.ScoutState;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;

/**
 * Created by rodrigo.jm.lourenco on 16/04/2015.
 */
public class Location {

    private final float nanos2Millis = 1 / 1000000f;
    private final float nanos2Seconds = 1 / 1000000000f;
    private long timestamp;
    private double latitude, longitude;
    private float altitude, barometricAltitude, speed, error;
    private int satellites;
    private long realElapsedTimeNanos;

    public Location() {
    }

    public Location(JsonObject location) {

        LocationState locationState = ScoutState.getInstance().getLocationState();

        this.timestamp = SensingUtils.LocationSampleAccessor.getTimestamp(location);
        this.realElapsedTimeNanos = SensingUtils.LocationSampleAccessor.getElapsedRealTimeNanos(location);
        this.error = SensingUtils.LocationSampleAccessor.getAccuracy(location);

        this.latitude = SensingUtils.LocationSampleAccessor.getLatitude(location);
        this.longitude = SensingUtils.LocationSampleAccessor.getLongitude(location);

        try {
            this.altitude = SensingUtils.LocationSampleAccessor.getAltitude(location);
        } catch (NoSuchDataFieldException e) {
            this.altitude = locationState.getAverageAltitude();
        }

        try {
            this.barometricAltitude = location.get(SensingUtils.LocationKeys.BAROMETRIC_ALTITUDE).getAsFloat();
        } catch (NullPointerException e) {
            this.barometricAltitude = locationState.getAverageBarometricAltitude();
        }

        try {
            this.speed = SensingUtils.LocationSampleAccessor.getSpeed(location);
        } catch (NoSuchDataFieldException e) {
            this.speed = locationState.getAverageSpeed();
        }

        try {
            this.satellites = SensingUtils.LocationSampleAccessor.getNumSatellites(location);
        } catch (NoSuchDataFieldException e) {
            this.satellites = 0;
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getSpeed() {
        return speed;
    }

    public float getAltitude() {
        return altitude;
    }

    public int getSatellites() {
        return satellites;
    }

    public double getTraveledDistance(Location l) {
        return LocationUtils.calculateDistance(
                this.latitude, this.longitude,
                l.getLatitude(), l.getLongitude());
    }

    public float getTraveledSpeed(Location l) {
        double distance = getTraveledDistance(l);
        long elapsedTimeSeconds =
                (long) Math.abs(((l.getRealElapsedTime() - this.getRealElapsedTime()) * nanos2Seconds));

        return (float) (distance / elapsedTimeSeconds);
    }

    /**
     * Given another location l, this method check if the uncertainty areas of the two locations
     * overlap.
     * <br>
     * The uncertainty area if defined by the accuracy field.
     *
     * @param l Location to compare to
     * @return True if the locations overlap, false otherwise.
     */
    public boolean isOverlapping(Location l) {

        //Two locations overlap if the distance between the two locations
        //is smaller than the sum of their radius.
        double distance = getTraveledDistance(l);
        float locationError = l.getErrorMargin();

        return distance < locationError + this.error;
    }

    public float getErrorMargin() {
        return error;
    }

    public long getRealElapsedTime() {
        return realElapsedTimeNanos;
    }

    public float getBarometricAltitude() {
        return barometricAltitude;
    }
}
