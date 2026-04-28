package ca.consmatt.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import ca.consmatt.beans.Account;
import ca.consmatt.beans.Friendship;
import ca.consmatt.beans.Location;
import ca.consmatt.beans.PostVisibility;
import ca.consmatt.dto.AccountResponse;
import ca.consmatt.dto.AccountUpdateResponse;
import ca.consmatt.dto.UpdateProfileRequest;
import ca.consmatt.repositories.AccountRepository;
import ca.consmatt.policy.UsernamePolicy;
import ca.consmatt.repositories.FriendshipRepository;
import ca.consmatt.repositories.LocationRepository;
import ca.consmatt.security.FishListJwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * REST API for user accounts: registration, profile lookup, and a user's locations.
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Validated
public class AccountController {

	private static final Logger log = LoggerFactory.getLogger(AccountController.class);

	private final AccountRepository accountRepository;
	private final LocationRepository locationRepository;
	private final FriendshipRepository friendshipRepository;
	private final FishListJwtService fishListJwtService;
	private final UsernamePolicy usernamePolicy;

	/**
	 * Returns the authenticated user's public profile.
	 *
	 * @param authentication current Spring Security principal
	 * @return id and username, or 404 if missing
	 */
	@GetMapping("/me")
	public ResponseEntity<AccountResponse> getCurrentAccount(Authentication authentication) {
		return accountRepository.findByUsername(authentication.getName())
				.map(a -> ResponseEntity.ok(toResponse(a)))
				.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Updates the authenticated user's profile (username and/or profile image key).
	 *
	 * @param request partial update; {@code profileImageKey} empty string clears the image
	 * @param authentication current user
	 * @return updated profile and a new JWT when anything changed
	 */
	@PatchMapping(path = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AccountUpdateResponse> patchMyProfile(
			@Valid @RequestBody UpdateProfileRequest request,
			Authentication authentication) {
		Account account = requireAccount(authentication);
		String usernameBefore = account.getUsername();
		boolean dirty = false;
		List<String> changes = new ArrayList<>();

		if (request.profileImageKey() != null) {
			String pk = request.profileImageKey();
			if (pk.isEmpty()) {
				account.setProfileImageKey(null);
				dirty = true;
				changes.add("profile_image=cleared");
			} else {
				assertValidUserUploadKey(pk, usernameBefore);
				account.setProfileImageKey(normalizeUploadKey(pk));
				dirty = true;
				changes.add("profile_image=set");
			}
		}

		if (request.username() != null) {
			String nu = request.username().trim();
			if (nu.isEmpty()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
			}
			if (!nu.matches("^[a-zA-Z0-9_]{2,100}$")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Username may only contain letters, numbers, and underscores (2–100 characters)");
			}
			usernamePolicy.validateProposedUsername(nu, account.getUsername());
			if (!nu.equals(account.getUsername())) {
				if (accountRepository.existsByUsername(nu)) {
					log.warn("PROFILE_UPDATE_FAILED user={} reason=username_taken requested={}", usernameBefore, nu);
					throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already taken");
				}
				account.setUsername(nu);
				dirty = true;
				changes.add("username=" + usernameBefore + "->" + nu);
			}
		}

		if (!dirty) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No changes");
		}

		accountRepository.save(account);
		log.info("PROFILE_UPDATE user={} changes={}", account.getUsername(), changes);
		String accessToken = fishListJwtService.createAccessToken(account.getUsername());
		return ResponseEntity.ok(new AccountUpdateResponse(toResponse(account), accessToken, "Bearer"));
	}

	/**
	 * Returns another user's public profile by numeric id.
	 *
	 * @param id account primary key
	 * @return id and username, or 404
	 */
	@GetMapping("/{id:\\d+}")
	public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id) {
		return accountRepository.findById(id)
				.map(a -> ResponseEntity.ok(toResponse(a)))
				.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Lists all fishing locations owned by the given account.
	 *
	 * @param id account primary key
	 * @return locations or 404 if the account does not exist
	 */
	@GetMapping("/{id:\\d+}/locations")
	public ResponseEntity<List<Location>> getAccountLocations(@PathVariable Long id,
			Authentication authentication) {
		if (!accountRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		Account viewer = requireAccount(authentication);
		List<Location> all = locationRepository.findByAccount_Id(id);
		if (viewer.getId().equals(id)) {
			return ResponseEntity.ok(all);
		}
		boolean friendsWithOwner = areFriends(viewer.getId(), id);
		List<Location> visible = all.stream()
				.filter(loc -> isLocationVisibleToViewer(loc.getVisibility(), friendsWithOwner))
				.collect(Collectors.toList());
		return ResponseEntity.ok(visible);
	}

	private static boolean isLocationVisibleToViewer(PostVisibility visibility, boolean friendsWithOwner) {
		if (visibility == null || visibility == PostVisibility.PUBLIC) {
			return true;
		}
		if (visibility == PostVisibility.PRIVATE) {
			return false;
		}
		return friendsWithOwner;
	}

	private boolean areFriends(Long accountIdA, Long accountIdB) {
		if (accountIdA.equals(accountIdB)) {
			return true;
		}
		long minId = Math.min(accountIdA, accountIdB);
		long maxId = Math.max(accountIdA, accountIdB);
		return friendshipRepository.findByAccountA_IdAndAccountB_Id(minId, maxId).isPresent();
	}

	/**
	 * Returns a small public account search result list for username lookups.
	 *
	 * @param query case-insensitive username fragment
	 * @param authentication current user
	 * @return up to 20 matching accounts, excluding the current user
	 */
	@GetMapping("/search")
	public List<AccountResponse> searchAccounts(
			@RequestParam(name = "query") @Size(min = 2, max = 100, message = "query length must be between 2 and 100") String query,
			Authentication authentication) {
		Account current = requireAccount(authentication);
		String normalized = query == null ? "" : query.trim();
		return accountRepository.findTop20ByUsernameContainingIgnoreCaseOrderByUsernameAsc(normalized).stream()
				.filter(a -> !a.getId().equals(current.getId()))
				.map(AccountController::toResponse)
				.toList();
	}

	/**
	 * Lists the authenticated user's friends.
	 *
	 * @param authentication current user
	 * @return friend summaries sorted by username
	 */
	@GetMapping("/me/friends")
	public List<AccountResponse> getMyFriends(Authentication authentication) {
		Account current = requireAccount(authentication);
		return friendshipRepository.findByAccountA_IdOrAccountB_Id(current.getId(), current.getId()).stream()
				.map(link -> {
					Account friend = link.getAccountA().getId().equals(current.getId()) ? link.getAccountB() : link.getAccountA();
					return toResponse(friend);
				})
				.distinct()
				.sorted((a, b) -> a.username().toLowerCase(Locale.ROOT).compareTo(b.username().toLowerCase(Locale.ROOT)))
				.toList();
	}

	/**
	 * Adds a symmetric friendship link between the current user and target account.
	 *
	 * @param id target account id
	 * @param authentication current user
	 * @return target account summary
	 */
	@PostMapping("/{id:\\d+}/friends")
	public AccountResponse addFriend(@PathVariable Long id, Authentication authentication) {
		Account current = requireAccount(authentication);
		Account target = accountRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
		if (current.getId().equals(target.getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot friend yourself");
		}

		Long minId = Math.min(current.getId(), target.getId());
		Long maxId = Math.max(current.getId(), target.getId());
		boolean exists = friendshipRepository.findByAccountA_IdAndAccountB_Id(minId, maxId).isPresent();
		if (!exists) {
			Account first = current.getId().equals(minId) ? current : target;
			Account second = current.getId().equals(minId) ? target : current;
			friendshipRepository.save(new Friendship(null, first, second, Instant.now().toString()));
			log.info("FRIEND_ADDED   actor={} target={}", current.getUsername(), target.getUsername());
		} else {
			log.info("FRIEND_NOOP    actor={} target={} reason=already_friends", current.getUsername(),
					target.getUsername());
		}
		return toResponse(target);
	}

	/**
	 * Removes a friendship link between the current user and target account.
	 *
	 * @param id target account id
	 * @param authentication current user
	 * @return 204 on success
	 */
	@DeleteMapping("/{id:\\d+}/friends")
	public ResponseEntity<Void> removeFriend(@PathVariable Long id, Authentication authentication) {
		Account current = requireAccount(authentication);
		if (current.getId().equals(id)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot unfriend yourself");
		}
		Long minId = Math.min(current.getId(), id);
		Long maxId = Math.max(current.getId(), id);
		boolean[] removed = { false };
		friendshipRepository.findByAccountA_IdAndAccountB_Id(minId, maxId)
				.ifPresent(link -> {
					friendshipRepository.delete(link);
					removed[0] = true;
				});
		if (removed[0]) {
			log.info("FRIEND_REMOVED actor={} targetId={}", current.getUsername(), id);
		} else {
			log.info("FRIEND_NOOP    actor={} targetId={} reason=not_friends", current.getUsername(), id);
		}
		return ResponseEntity.noContent().build();
	}

	private Account requireAccount(Authentication authentication) {
		return accountRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}

	private static AccountResponse toResponse(Account a) {
		return new AccountResponse(a.getId(), a.getUsername(), a.getProfileImageKey());
	}

	private static String normalizeUploadKey(String objectKey) {
		String normalized = objectKey.trim();
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		return normalized;
	}

	private static void assertValidUserUploadKey(String raw, String usernameAtUpload) {
		if (raw == null || raw.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid object key");
		}
		String key = normalizeUploadKey(raw);
		if (key.contains("..")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid object key");
		}
		if (!key.startsWith("uploads/")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid object key");
		}
		String prefix = "uploads/" + usernameAtUpload + "/";
		if (!key.startsWith(prefix)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Profile image must be uploaded while signed in as this account (expected key under " + prefix + ")");
		}
	}
}
