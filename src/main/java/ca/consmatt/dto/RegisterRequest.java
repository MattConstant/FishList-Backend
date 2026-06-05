package ca.consmatt.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-service signup with username + password for {@code POST /api/auth/register}.
 */
public record RegisterRequest(
		@NotBlank @Size(max = 100) String username,
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 8, max = 200) String password) {
}
