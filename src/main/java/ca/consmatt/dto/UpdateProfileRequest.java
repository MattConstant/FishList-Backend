package ca.consmatt.dto;

import jakarta.validation.constraints.Size;

/**
 * Partial profile update. Omit a field to leave it unchanged; set {@code profileImageKey} to
 * empty string to remove the profile image.
 */
public record UpdateProfileRequest(
		@Size(min = 2, max = 100) String username,
		@Size(max = 512) String profileImageKey) {
}
