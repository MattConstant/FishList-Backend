package ca.consmatt.dto;

import ca.consmatt.beans.PostVisibility;

/**
 * Flattened forum thread row for the home feed.
 */
public record ForumThreadResponse(
		Long id,
		Long accountId,
		String username,
		String title,
		String body,
		String createdAt,
		PostVisibility visibility,
		long commentsCount,
		long likesCount) {
}
