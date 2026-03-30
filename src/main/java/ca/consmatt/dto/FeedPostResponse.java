package ca.consmatt.dto;

/**
 * Flattened feed row used for paginated latest-post queries.
 */
public record FeedPostResponse(
		Long locationId,
		String locationName,
		String latitude,
		String longitude,
		String timeStamp,
		Long accountId,
		String username,
		Long catchId,
		String species,
		Integer quantity,
		Double lengthCm,
		Double weightKg,
		String notes,
		String description,
		String imageUrl,
		String imageUrls) {
}
