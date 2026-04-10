package ca.consmatt.dto;

import ca.consmatt.beans.Fish;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating or updating a {@link ca.consmatt.beans.Fish} catalog row (API contract; not a JPA entity).
 */
public record CreateFishRequest(
		@NotBlank(message = "species is required")
		@Size(max = 120, message = "species must be at most 120 characters")
		String species,
		@Positive(message = "averageWeight must be positive")
		Double averageWeight,
		@Positive(message = "averageLength must be positive")
		Double averageLength,
		@Positive(message = "maxWeight must be positive")
		Double maxWeight,
		@Positive(message = "maxLength must be positive")
		Double maxLength,
		@Size(max = 2048, message = "imageUrl must be at most 2048 characters")
		String imageUrl,
		@Size(max = 1000, message = "description must be at most 1000 characters")
		String description) {

	/** Maps this request to a new persistent {@link ca.consmatt.beans.Fish} (no id; assigned on save). */
	public Fish toNewEntity() {
		return Fish.builder()
				.species(species)
				.averageWeight(averageWeight)
				.averageLength(averageLength)
				.maxWeight(maxWeight)
				.maxLength(maxLength)
				.imageUrl(imageUrl)
				.description(description)
				.build();
	}

	/** Copies fields onto an existing managed {@link ca.consmatt.beans.Fish} (e.g. update by id). */
	public void applyTo(Fish target) {
		target.setSpecies(species);
		target.setAverageWeight(averageWeight);
		target.setAverageLength(averageLength);
		target.setMaxWeight(maxWeight);
		target.setMaxLength(maxLength);
		target.setImageUrl(imageUrl);
		target.setDescription(description);
	}
}
