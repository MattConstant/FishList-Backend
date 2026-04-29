package ca.consmatt.dto;

import java.util.List;

import ca.consmatt.beans.Catch;

/**
 * Wraps a saved {@link Catch} together with any achievements unlocked by the same request.
 * Inline delivery means the client can show the toast without an extra round-trip.
 *
 * @param catchRecord the persisted catch (serialized as {@code "catch"} via the explicit getter)
 * @param unlockedAchievements achievements newly earned during this call (empty when none)
 */
public record AddCatchResponse(
		@com.fasterxml.jackson.annotation.JsonProperty("catch") Catch catchRecord,
		List<UnlockedAchievementSummary> unlockedAchievements) {
}
