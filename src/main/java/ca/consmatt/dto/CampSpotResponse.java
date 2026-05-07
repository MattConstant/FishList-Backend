package ca.consmatt.dto;

import java.util.List;

import ca.consmatt.beans.PostVisibility;

/**
 * Public view of a camp spot marker for the map.
 */
public record CampSpotResponse(
		Long id,
		String name,
		String latitude,
		String longitude,
		String timeStamp,
		Long accountId,
		String username,
		PostVisibility visibility,
		List<String> imageUrls) {
}

