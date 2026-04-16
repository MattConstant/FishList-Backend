package ca.consmatt.dto;

/**
 * Result of PATCH /api/accounts/me: updated public profile and a fresh JWT (subject is username).
 */
public record AccountUpdateResponse(AccountResponse account, String accessToken, String tokenType) {
}
