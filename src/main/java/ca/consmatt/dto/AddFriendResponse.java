package ca.consmatt.dto;

import java.util.List;

/**
 * Adds an inline list of newly unlocked achievements to the friend-add response.
 *
 * @param friend public profile of the newly added friend
 * @param unlockedAchievements achievements earned by this action (empty when none)
 */
public record AddFriendResponse(
		AccountResponse friend,
		List<UnlockedAchievementSummary> unlockedAchievements) {
}
