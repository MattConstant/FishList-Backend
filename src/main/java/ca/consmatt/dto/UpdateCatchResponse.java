package ca.consmatt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import ca.consmatt.beans.Catch;
import ca.consmatt.beans.PostVisibility;

/**
 * Updated feed post fields after a catch edit.
 */
public record UpdateCatchResponse(
		Long locationId,
		String locationName,
		PostVisibility visibility,
		@JsonProperty("catch") Catch catchRecord) {
}
