package ca.consmatt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-service signup with username + password for {@code POST /api/auth/register}.
 */
public record RegisterRequest(
		@NotBlank @Size(max = 100) String username,
		@NotBlank @Size(min = 8, max = 200) String password) {
}
