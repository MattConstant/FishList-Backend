package ca.consmatt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One fish line inside a multi-fish catch (or the single line when using {@code fish} array).
 */
public record FishEntryRequest(
		@NotBlank @Size(max = 200) String species,
		Double lengthCm,
		Double weightKg,
		@Size(max = 500) String notes) {
}
