package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.location;

/**
 * Collection of functions to support location-based calculations.
 *
 * @version 1.0
 * @author rodrigo.jm.lourenco
 */
public class LocationUtils {

    public static final float PRESSURE_STANDARD_ATMOSPHERE = 1013.25f;

    public static enum DistanceStrategies {
        Haversine,
        SphericalLawOfCosines
    }

    /** The Earth's Radius in meters according to <a href="http://en.wikipedia.org/wiki/Earth_radius">Wikipedia</a>*/
    public static final double R = 6371*1000; // In meters

    /**
     * Returns the distance, in meters, between two geo-coordinates using the Haversine Formula.
     *
     * The haversine formula is an equation important in navigation, giving great-circle distances
     * between two points on a sphere from their longitudes and latitudes. It is a special case of a
     * more general formula in spherical trigonometry, the law of haversines, relating the sides and
     * angles of spherical "triangles".
     * <h3>References</h3>
     * <a href="http://rosettacode.org/wiki/Haversine_formula">Haversine Formula</a>
     *
     * @param fromLat Latitude from point 1
     * @param fromLon Longitude from point 1
     * @param toLat Latitude from point 2
     * @param toLon Longitude from point 2
     * @return Distance between point 1 and 2
     */
    public static double haversineFormula(double fromLat, double fromLon, double toLat, double toLon) {
        double dLat = Math.toRadians(toLat - fromLat);
        double dLon = Math.toRadians(toLat - fromLon);
        fromLat = Math.toRadians(fromLat);
        toLat = Math.toRadians(toLat);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) *
                Math.cos(fromLat) * Math.cos(toLat);

        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }

    /**
     * Returns the distance, in meters, between two geo-coordinates using the Spherical Law of Cosines.
     *
     * @param fromLat Latitude from point 1
     * @param fromLon Longitude from point 1
     * @param toLat Latitude from point 2
     * @param toLon Longitude from point 2
     * @return Distance between point 1 and 2
     */
    public static double sphericalLawOfCosines(double fromLat, double fromLon, double toLat, double toLon){

        double deltaLon = Math.toRadians(toLon-fromLon);

        fromLat = Math.toRadians(fromLat);
        fromLon = Math.toRadians(fromLon);
        toLat   = Math.toRadians(toLat);
        toLon   = Math.toRadians(toLon);

        double d =
                Math.acos(Math.sin(fromLat) *
                Math.sin(toLat) + Math.cos(fromLat) *
                Math.cos(toLat) * Math.cos(deltaLon));

        return d*R;
    }


    /**
     * Returns the distance, in meters, between two geo-coordinates using the specified strategy.
     *
     * @param strategy Defines the strategy used to calculate the distance
     * @param fromLat Latitude from point 1
     * @param fromLon Longitude from point 1
     * @param toLat Latitude from point 2
     * @param toLon Longitude from point 2
     * @return Distance between point 1 and 2
     */
    public static double calculateDistance(DistanceStrategies strategy, double fromLat, double fromLon, double toLat, double toLon ){

        switch (strategy){
            case Haversine:
                return haversineFormula(fromLat, fromLon, toLat, toLon);
            case SphericalLawOfCosines:
            default:
                return sphericalLawOfCosines(fromLat, fromLon, toLat, toLon);

        }
    }

    /**
     * Returns the distance, in meters, between two geo-coordinates. By default the Spherical Law
     * of Cosines is employed.
     *
     * @param fromLat Latitude from point 1
     * @param fromLon Longitude from point 1
     * @param toLat Latitude from point 2
     * @param toLon Longitude from point 2
     * @return Distance between point 1 and 2
     */
    public static double calculateDistance(double fromLat, double fromLon, double toLat, double toLon ){
        return calculateDistance(DistanceStrategies.SphericalLawOfCosines,
                fromLat, fromLon,
                toLat, toLon);
    }

    /**
     *
     * @param distance
     * @param fromAlt
     * @param toAlt
     * @return
     */
    public static float calculateSlope(double distance, double fromAlt, double toAlt){

        double deltaAlt = toAlt -fromAlt;

        //If no distance was travelled then there is no slope
        return distance == 0 ? 0 : (float) ((float)deltaAlt/distance);

    }

    /**
     *
     * @param fromLat
     * @param fromLon
     * @param fromAlt
     * @param toLat
     * @param toLon
     * @param toAlt
     * @return
     */
    public static float calculateSlope(double fromLat, double fromLon, double fromAlt,
                                       double toLat, double toLon, double toAlt){

        double distance = calculateDistance(fromLat, fromLon, toLat, toLon);
        return calculateSlope(distance, fromAlt, toAlt);

    }

    public static double convertToKilometersPerHour(float speedMetersPerSecond){
        return speedMetersPerSecond * 3.6;
    }

    //TODO: este código é da Android, estarei a violar a licença?
    public static float getAltitude(float p0, float p){
        final float coef = 1.0f / 5.255f;
        return 44330.0f * (1.0f - (float)Math.pow(p/p0, coef));
    }

}
