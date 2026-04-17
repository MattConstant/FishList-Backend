package ca.consmatt.util;

/**
 * Small geodesic helpers (WGS84 sphere).
 */
public final class GeoUtils {

	private static final double EARTH_RADIUS_M = 6_371_000.0;

	private GeoUtils() {
	}

	/**
	 * @param latDeg  latitude in degrees
	 * @param lngDeg  longitude in degrees
	 * @param bearingDeg clockwise from north, degrees
	 * @param distanceM distance in meters
	 * @return [latitudeDeg, longitudeDeg]
	 */
	public static double[] destinationDegrees(double latDeg, double lngDeg, double bearingDeg, double distanceM) {
		double lat1 = Math.toRadians(latDeg);
		double lng1 = Math.toRadians(lngDeg);
		double brng = Math.toRadians(bearingDeg);
		double dr = distanceM / EARTH_RADIUS_M;

		double lat2 = Math.asin(
				Math.sin(lat1) * Math.cos(dr) + Math.cos(lat1) * Math.sin(dr) * Math.cos(brng));
		double lng2 = lng1 + Math.atan2(
				Math.sin(brng) * Math.sin(dr) * Math.cos(lat1),
				Math.cos(dr) - Math.sin(lat1) * Math.sin(lat2));

		return new double[] { Math.toDegrees(lat2), Math.toDegrees(lng2) };
	}
}
