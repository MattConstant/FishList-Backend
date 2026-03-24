package ca.consmatt.dto;

/**
 * Public account view (no password).
 *
 * @param id account primary key
 * @param username login name
 */
public record AccountResponse(Long id, String username) {
}
