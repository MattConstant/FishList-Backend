package ca.consmatt.policy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import ca.consmatt.config.ContentPolicyProperties;
import ca.consmatt.security.AdminProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Validates proposed usernames: reserved admin names and banned language (substring checks).
 * Keep default banned fragments in sync with {@code fishlist-frontend/src/lib/username-policy.ts}.
 */
@Component
@RequiredArgsConstructor
public class UsernamePolicy {

	public static final String REJECT_REASON_RESERVED = "USERNAME_RESERVED";
	public static final String REJECT_REASON_INAPPROPRIATE = "USERNAME_INAPPROPRIATE";

	private static final List<String> DEFAULT_BANNED_SUBSTRINGS = List.of(
			"fuck", "shit", "bitch", "cunt", "nigg", "fagg", "rape", "pedo", "porn",
			"slut", "whore", "cock", "dick", "anus", "tard", "coon", "spic", "chink",
			"dyke", "homo", "jizz", "puss", "scum", "twat", "wank");

	private final AdminProperties adminProperties;
	private final ContentPolicyProperties contentPolicyProperties;

	private List<String> mergedBannedLowercase = List.of();

	@PostConstruct
	void mergeBannedLists() {
		Set<String> set = new LinkedHashSet<>();
		for (String s : DEFAULT_BANNED_SUBSTRINGS) {
			addFragment(set, s);
		}
		if (contentPolicyProperties.getBannedUsernameSubstrings() != null) {
			for (String s : contentPolicyProperties.getBannedUsernameSubstrings()) {
				addFragment(set, s);
			}
		}
		mergedBannedLowercase = new ArrayList<>(set);
	}

	private static void addFragment(Set<String> set, String raw) {
		if (raw == null) {
			return;
		}
		String t = raw.trim().toLowerCase(Locale.ROOT);
		if (!t.isEmpty()) {
			set.add(t);
		}
	}

	/**
	 * @param proposed username candidate (already format-validated elsewhere)
	 * @param currentUsername {@code null} for new registration; otherwise existing name (allows keeping same)
	 */
	public void validateProposedUsername(String proposed, String currentUsername) {
		if (proposed == null || proposed.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
		}
		if (currentUsername != null && proposed.equalsIgnoreCase(currentUsername)) {
			return;
		}
		if (isReservedAdminName(proposed, currentUsername)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, REJECT_REASON_RESERVED);
		}
		if (containsBannedSubstring(proposed)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, REJECT_REASON_INAPPROPRIATE);
		}
	}

	/** True if acceptable for a brand-new account username (registration / derived name). */
	public boolean isAcceptableNewAccountUsername(String candidate) {
		if (candidate == null || candidate.isBlank()) {
			return false;
		}
		if (adminProperties.isAdminUsername(candidate)) {
			return false;
		}
		return !containsBannedSubstring(candidate);
	}

	private boolean isReservedAdminName(String proposed, String currentUsername) {
		if (!adminProperties.isAdminUsername(proposed)) {
			return false;
		}
		return currentUsername == null || !proposed.equalsIgnoreCase(currentUsername);
	}

	private boolean containsBannedSubstring(String username) {
		String lower = username.toLowerCase(Locale.ROOT);
		for (String fragment : mergedBannedLowercase) {
			if (lower.contains(fragment)) {
				return true;
			}
		}
		return false;
	}
}
