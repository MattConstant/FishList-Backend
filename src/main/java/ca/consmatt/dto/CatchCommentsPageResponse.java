package ca.consmatt.dto;

import java.util.List;

/**
 * Paginated comments for a catch.
 */
public record CatchCommentsPageResponse(
		List<CatchCommentResponse> comments,
		long totalCount,
		int offset,
		int limit) {
}
