package ca.consmatt.dto;

/**
 * Like status for one catch, as seen by the current user.
 */
public record CatchLikeResponse(
		Long catchId,
		long likesCount,
		boolean likedByMe) {
}
