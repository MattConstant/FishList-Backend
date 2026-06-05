package ca.consmatt.dto;

import java.util.List;

import ca.consmatt.beans.FishingType;
import ca.consmatt.beans.PostVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for editing a feed post (catch text + location visibility/name). Photos are unchanged.
 */
public record UpdateCatchRequest(
		@NotBlank @Size(max = 120) String locationName,
		@NotNull PostVisibility visibility,
		FishingType fishingType,
		@Size(max = 2048) String description,
		@NotEmpty @Size(max = 30) List<@Valid FishEntryRequest> fish) {
}
