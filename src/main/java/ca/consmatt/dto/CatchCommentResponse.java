package ca.consmatt.dto;

/**
 * API response for one catch comment.
 */
public record CatchCommentResponse(
		Long id,
		Long catchId,
		Long accountId,
		String username,
		String message,
		String createdAt,
		boolean ownedByMe) {
}
