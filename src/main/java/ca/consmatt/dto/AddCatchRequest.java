package ca.consmatt.dto;

import java.util.List;

import ca.consmatt.beans.FishingType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a {@link ca.consmatt.beans.Catch} under a location.
 *
 * @param species species or common name (required if {@code fish} is null or empty)
 * @param quantity optional count; defaults to 1 for a single-fish catch
 * @param lengthCm optional length in centimeters (single-fish / legacy)
 * @param weightKg optional weight in kilograms (single-fish / legacy)
 * @param notes optional short notes (single-fish / legacy)
 * @param imageUrl optional photo URL
 * @param imageUrls optional list of up to 4 photo URLs/keys
 * @param description optional longer text
 * @param fishingType optional technique used (fly fishing, trolling, etc.)
 * @param fish when non-empty, per-fish details for one post; multiple entries stored in
 *        {@code fishDetailsJson} on the catch
 */
public record AddCatchRequest(
		String species,
		@Min(1) Integer quantity,
		Double lengthCm,
		Double weightKg,
		String notes,
		String imageUrl,
		@Size(max = 4) List<String> imageUrls,
		String description,
		FishingType fishingType,
		@Size(max = 30) List<@Valid FishEntryRequest> fish) {
}
