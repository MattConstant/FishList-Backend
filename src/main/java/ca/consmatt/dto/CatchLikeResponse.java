package ca.consmatt.dto;

import java.util.List;

/**
 * Like status for one catch, as seen by the current user.
 * {@link #unlockedAchievements()} is empty unless this request unlocked an achievement
 * (typically only on the first like in the user's lifetime, or when crossing a threshold).
 */
public record CatchLikeResponse(
		Long catchId,
		long likesCount,
		boolean likedByMe,
		List<UnlockedAchievementSummary> unlockedAchievements) {

	public CatchLikeResponse(Long catchId, long likesCount, boolean likedByMe) {
		this(catchId, likesCount, likedByMe, List.of());
	}
}
