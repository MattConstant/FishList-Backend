package ca.consmatt.dto;

import java.util.List;

import ca.consmatt.beans.PostVisibility;
import ca.consmatt.beans.CampSpot;
import ca.consmatt.beans.Account;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new camp spot.
 */
public record CreateCampSpotRequest(
		@NotBlank(message = "name is required") @Size(max = 120, message = "name must be at most 120 characters") String name,
		@NotBlank(message = "latitude is required") @Size(max = 40, message = "latitude must be at most 40 characters") String latitude,
		@NotBlank(message = "longitude is required") @Size(max = 40, message = "longitude must be at most 40 characters") String longitude,
		@NotBlank(message = "timeStamp is required") @Size(max = 80, message = "timeStamp must be at most 80 characters") String timeStamp,
		PostVisibility visibility,
		List<@Size(max = 600, message = "imageUrls items must be at most 600 characters") String> imageUrls) {

	public CampSpot toNewEntity(Account owner) {
		CampSpot c = new CampSpot(
				name.trim(),
				latitude.trim(),
				longitude.trim(),
				timeStamp.trim());
		c.setAccount(owner);
		c.setVisibility(visibility);
		if (imageUrls != null) {
			c.setImageUrls(imageUrls.stream()
					.filter(s -> s != null && !s.isBlank())
					.map(String::trim)
					.limit(4)
					.toList());
		}
		return c;
	}
}

