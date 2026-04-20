package ca.consmatt.dto;

import ca.consmatt.beans.Account;
import ca.consmatt.beans.Location;
import ca.consmatt.beans.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating or updating a {@link ca.consmatt.beans.Location} (API contract; not a JPA entity).
 * Owner {@link Account} is set server-side from authentication.
 */
public record CreateLocationRequest(
		@NotBlank(message = "locationName is required")
		@Size(max = 120, message = "locationName must be at most 120 characters")
		String locationName,
		@NotBlank(message = "latitude is required")
		@Size(max = 40, message = "latitude must be at most 40 characters")
		String latitude,
		@NotBlank(message = "longitude is required")
		@Size(max = 40, message = "longitude must be at most 40 characters")
		String longitude,
		@NotBlank(message = "timeStamp is required")
		@Size(max = 80, message = "timeStamp must be at most 80 characters")
		String timeStamp,
		@Size(max = 1000, message = "details must be at most 1000 characters")
		String details,
		PostVisibility visibility) {

	/** Builds a new {@link Location} with the given owner (id assigned on save). */
	public Location toNewLocation(Account account) {
		Location location = new Location();
		location.setLocationName(locationName);
		location.setLatitude(latitude);
		location.setLongitude(longitude);
		location.setTimeStamp(timeStamp);
		location.setDetails(details);
		location.setVisibility(visibility != null ? visibility : PostVisibility.PUBLIC);
		location.setAccount(account);
		return location;
	}

	/** Copies mutable fields onto an existing managed {@link Location} (e.g. update by id). */
	public void applyTo(Location target) {
		target.setLocationName(locationName);
		target.setLatitude(latitude);
		target.setLongitude(longitude);
		target.setTimeStamp(timeStamp);
		target.setDetails(details);
		if (visibility != null) {
			target.setVisibility(visibility);
		}
	}
}
