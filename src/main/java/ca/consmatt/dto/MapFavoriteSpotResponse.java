package ca.consmatt.dto;

/**
 * Serialized map bookmark for {@code GET/POST .../map-favorites}.
 */
public record MapFavoriteSpotResponse(
		long id,
		double latitude,
		double longitude,
		String spotKey,
		String label,
		long createdAtEpochMs) {
}
