package ca.consmatt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Username + password for {@code POST /api/auth/login}.
 */
public record PasswordLoginRequest(
		@NotBlank @Size(max = 100) String username,
		@NotBlank @Size(max = 200) String password) {
}
