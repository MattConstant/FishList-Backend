package ca.consmatt.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * GIS credential (Google ID token JWT string).
 */
public record GoogleAuthRequest(@NotBlank String credential) {
}
