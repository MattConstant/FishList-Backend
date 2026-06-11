package ca.consmatt.dto;

/**
 * A reply to a comment you wrote on a forum thread. Used for in-app notifications.
 */
public record ThreadCommentReplyNotificationResponse(
		long replyCommentId,
		long threadId,
		String threadTitle,
		String replierUsername,
		String messagePreview,
		String createdAt) {
}
