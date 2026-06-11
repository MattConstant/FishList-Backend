package ca.consmatt.dto;

import ca.consmatt.beans.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for editing a forum thread (owner only).
 */
public record UpdateForumThreadRequest(
		@NotBlank(message = "title is required")
		@Size(max = 200, message = "title must be at most 200 characters")
		String title,
		@NotBlank(message = "body is required")
		@Size(max = 5000, message = "body must be at most 5000 characters")
		String body,
		PostVisibility visibility) {
}
