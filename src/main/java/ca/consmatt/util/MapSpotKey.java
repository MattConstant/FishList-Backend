package ca.consmatt.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Five-decimal spot key aligned with {@code fishlist-frontend/src/lib/map-favorites.ts}. */
public final class MapSpotKey {

	private static final int SCALE = 5;

	private MapSpotKey() {
	}

	public static double snap(double n) {
		return BigDecimal.valueOf(n).setScale(SCALE, RoundingMode.HALF_UP).doubleValue();
	}

	public static String spotKey(double latitude, double longitude) {
		double slat = snap(latitude);
		double slng = snap(longitude);
		return String.format(Locale.US, "%.5f|%.5f", slat, slng);
	}
}
