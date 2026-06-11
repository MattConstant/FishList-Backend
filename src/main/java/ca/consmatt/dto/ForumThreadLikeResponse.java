package ca.consmatt.dto;

import java.util.List;

/**
 * Like status for one forum thread, as seen by the current user.
 */
public record ForumThreadLikeResponse(
		Long threadId,
		long likesCount,
		boolean likedByMe,
		List<UnlockedAchievementSummary> unlockedAchievements) {

	public ForumThreadLikeResponse(Long threadId, long likesCount, boolean likedByMe) {
		this(threadId, likesCount, likedByMe, List.of());
	}
}
