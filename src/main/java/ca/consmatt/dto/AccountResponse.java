package ca.consmatt.dto;

/**
 * Public account view (no password).
 *
 * @param id account primary key
 * @param username login name
 * @param profileImageKey optional object storage key for profile image
 */
public record AccountResponse(Long id, String username, String profileImageKey) {
}
