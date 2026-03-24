package ca.consmatt.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for creating a {@link ca.consmatt.beans.Catch} under a location.
 *
 * @param species required species or common name
 * @param quantity optional count; defaults to 1 in controller if null
 * @param lengthCm optional length in centimeters
 * @param weightKg optional weight in kilograms
 * @param notes optional short notes
 * @param imageUrl optional photo URL
 * @param description optional longer text
 */
public record AddCatchRequest(
		@NotBlank String species,
		@Min(1) Integer quantity,
		Double lengthCm,
		Double weightKg,
		String notes,
		String imageUrl,
		String description) {
}
