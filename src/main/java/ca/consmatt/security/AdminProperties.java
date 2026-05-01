package ca.consmatt.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configured admin usernames allowed to access admin endpoints.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {

	/**
	 * Usernames that receive admin API access and {@code ADMIN} promotion on startup.
	 */
	private List<String> usernames = new ArrayList<>();

	/**
	 * Extra usernames no account may take (exact match, case-insensitive). Does not grant admin access.
	 */
	private List<String> reservedUsernames = new ArrayList<>();

	public boolean hasConfiguredAdmins() {
		return !normalizedUsernames().isEmpty();
	}

	/** True if {@code username} is listed in {@link #usernames} (admin privileges). */
	public boolean isAdminUsername(String username) {
		if (username == null || username.isBlank()) {
			return false;
		}
		return normalizedUsernames().contains(username.trim().toLowerCase(Locale.ROOT));
	}

	/**
	 * True if {@code username} is reserved from config: {@link #usernames} or {@link #reservedUsernames}.
	 */
	public boolean isUsernameReservedFromConfig(String username) {
		if (username == null || username.isBlank()) {
			return false;
		}
		String n = username.trim().toLowerCase(Locale.ROOT);
		return normalizedUsernames().contains(n) || normalizedReservedUsernames().contains(n);
	}

	private Set<String> normalizedUsernames() {
		return usernames.stream()
				.map(s -> s == null ? "" : s.trim().toLowerCase(Locale.ROOT))
				.filter(s -> !s.isBlank())
				.collect(Collectors.toSet());
	}

	private Set<String> normalizedReservedUsernames() {
		return reservedUsernames.stream()
				.map(s -> s == null ? "" : s.trim().toLowerCase(Locale.ROOT))
				.filter(s -> !s.isBlank())
				.collect(Collectors.toSet());
	}
}
