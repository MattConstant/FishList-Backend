package ca.consmatt.dto;

/**
 * A reply to a comment you wrote (on any catch you can see). Used for the in-app notification feed.
 */
public record CommentReplyNotificationResponse(
		long replyCommentId,
		long locationId,
		long catchId,
		String replierUsername,
		String messagePreview,
		String createdAt) {
}
