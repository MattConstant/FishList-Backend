package ca.consmatt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for self-registration.
 *
 * @param username unique handle, max 100 chars
 * @param password plain password, 8–200 chars
 */
public record CreateAccountRequest(
		@NotBlank @Size(max = 100) String username,
		@NotBlank @Size(min = 8, max = 200) String password) {
}
