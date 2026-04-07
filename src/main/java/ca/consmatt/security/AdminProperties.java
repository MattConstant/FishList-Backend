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

	private List<String> usernames = new ArrayList<>();

	public boolean hasConfiguredAdmins() {
		return !normalizedUsernames().isEmpty();
	}

	public boolean isAdminUsername(String username) {
		if (username == null || username.isBlank()) {
			return false;
		}
		return normalizedUsernames().contains(username.trim().toLowerCase(Locale.ROOT));
	}

	private Set<String> normalizedUsernames() {
		return usernames.stream()
				.map(s -> s == null ? "" : s.trim().toLowerCase(Locale.ROOT))
				.filter(s -> !s.isBlank())
				.collect(Collectors.toSet());
	}
}
