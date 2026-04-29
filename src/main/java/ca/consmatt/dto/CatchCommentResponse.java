package ca.consmatt.dto;

import java.util.List;

/**
 * API response for one catch comment.
 * {@link #unlockedAchievements()} is non-empty only on the request that earned a new achievement
 * (e.g. the user's first comment ever).
 */
public record CatchCommentResponse(
		Long id,
		Long catchId,
		Long accountId,
		String username,
		String message,
		String createdAt,
		boolean ownedByMe,
		List<UnlockedAchievementSummary> unlockedAchievements) {

	public CatchCommentResponse(Long id, Long catchId, Long accountId, String username,
			String message, String createdAt, boolean ownedByMe) {
		this(id, catchId, accountId, username, message, createdAt, ownedByMe, List.of());
	}
}
