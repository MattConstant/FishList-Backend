package ca.consmatt.dto;

import java.util.List;

/**
 * Paginated comments for one forum thread.
 */
public record ForumThreadCommentsPageResponse(
		List<ForumThreadCommentResponse> comments,
		long totalCount,
		int offset,
		int limit) {
}
