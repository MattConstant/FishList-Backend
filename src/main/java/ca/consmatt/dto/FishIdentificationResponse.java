package ca.consmatt.dto;

/**
 * Response payload for AI fish identification requests.
 *
 * @param suggestedSpecies best guess species name
 * @param confidence confidence score in range 0..1 when available
 * @param message optional model note (e.g., uncertainty/no fish)
 */
public record FishIdentificationResponse(
		String suggestedSpecies,
		Double confidence,
		String message) {
}
