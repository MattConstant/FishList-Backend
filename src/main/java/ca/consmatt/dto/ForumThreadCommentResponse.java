package ca.consmatt.dto;

import java.util.List;

/**
 * Comment on a forum thread, as returned to the client.
 */
public record ForumThreadCommentResponse(
		Long id,
		Long threadId,
		Long accountId,
		String username,
		String message,
		String createdAt,
		boolean ownedByMe,
		List<UnlockedAchievementSummary> unlockedAchievements,
		Long parentCommentId,
		String inReplyToUsername) {

	public ForumThreadCommentResponse(Long id, Long threadId, Long accountId, String username,
			String message, String createdAt, boolean ownedByMe,
			Long parentCommentId, String inReplyToUsername) {
		this(id, threadId, accountId, username, message, createdAt, ownedByMe, List.of(),
				parentCommentId, inReplyToUsername);
	}
}
