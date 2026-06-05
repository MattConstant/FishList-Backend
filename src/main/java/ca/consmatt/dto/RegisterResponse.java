package ca.consmatt.dto;

/**
 * Returned after password registration when email confirmation is required before login.
 */
public record RegisterResponse(
		String message,
		String email,
		boolean emailVerificationRequired) {
}
