package ca.consmatt.dto;

/**
 * Public account view (no password).
 *
 * @param id account primary key
 * @param username login name
 * @param profileImageKey optional object storage key for profile image
 * @param hasRegisteredEmail {@code true} when the account has an email on file (only on {@code /me})
 * @param emailHint masked email for display (only on {@code /me})
 */
public record AccountResponse(
		Long id,
		String username,
		String profileImageKey,
		Boolean hasRegisteredEmail,
		String emailHint) {

	public AccountResponse(Long id, String username, String profileImageKey) {
		this(id, username, profileImageKey, null, null);
	}
}
