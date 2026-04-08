package ca.consmatt.dto;

/**
 * FishList API token after successful Google ID token verification.
 */
public record GoogleAuthResponse(String accessToken, String tokenType, AccountResponse account) {
}
