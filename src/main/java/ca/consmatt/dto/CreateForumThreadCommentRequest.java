package ca.consmatt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a comment on a forum thread.
 */
public record CreateForumThreadCommentRequest(
		@NotBlank(message = "message is required")
		@Size(max = 500, message = "message must be at most 500 characters")
		String message,
		Long parentCommentId) {
}
