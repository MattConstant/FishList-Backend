package ca.consmatt.dto;

import java.util.List;

import ca.consmatt.beans.Catch;

/**
 * Location plus ordered catch list for detail API responses.
 *
 * @param id location id
 * @param locationName display name
 * @param latitude latitude string
 * @param longitude longitude string
 * @param timeStamp when recorded (application-defined format)
 * @param details optional extra text
 * @param accountId owning user id
 * @param catches catches at this spot
 */
public record LocationDetailResponse(
		Long id,
		String locationName,
		String latitude,
		String longitude,
		String timeStamp,
		String details,
		Long accountId,
		List<Catch> catches) {

	/**
	 * Maps a {@link ca.consmatt.beans.Location} and its catches into this record.
	 *
	 * @param location persisted location
	 * @param catches same-location catches (typically ordered)
	 * @return aggregate response
	 */
	public static LocationDetailResponse from(ca.consmatt.beans.Location location, List<Catch> catches) {
		return new LocationDetailResponse(
				location.getId(),
				location.getLocationName(),
				location.getLatitude(),
				location.getLongitude(),
				location.getTimeStamp(),
				location.getDetails(),
				location.getAccountId(),
				catches);
	}
}
