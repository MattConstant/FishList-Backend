package ca.consmatt.controllers;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import ca.consmatt.beans.Account;
import ca.consmatt.beans.Friendship;
import ca.consmatt.beans.Location;
import ca.consmatt.dto.AccountResponse;
import ca.consmatt.dto.CreateAccountRequest;
import ca.consmatt.repositories.AccountRepository;
import ca.consmatt.repositories.FriendshipRepository;
import ca.consmatt.repositories.LocationRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * REST API for user accounts: registration, profile lookup, and a user's locations.
 */
@RestController
@RequestMapping("/api/accounts")
@CrossOrigin
@RequiredArgsConstructor
@Validated
public class AccountController {

	private final AccountRepository accountRepository;
	private final LocationRepository locationRepository;
	private final FriendshipRepository friendshipRepository;
	private final PasswordEncoder passwordEncoder;

	/**
	 * Returns the authenticated user's public profile.
	 *
	 * @param authentication current Spring Security principal
	 * @return id and username, or 404 if missing
	 */
	@GetMapping("/me")
	public ResponseEntity<AccountResponse> getCurrentAccount(Authentication authentication) {
		return accountRepository.findByUsername(authentication.getName())
				.map(a -> ResponseEntity.ok(new AccountResponse(a.getId(), a.getUsername())))
				.orElse(ResponseEntity.notFound().build());
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
				.map(a -> ResponseEntity.ok(new AccountResponse(a.getId(), a.getUsername())))
				.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Lists all fishing locations owned by the given account.
	 *
	 * @param id account primary key
	 * @return locations or 404 if the account does not exist
	 */
	@GetMapping("/{id:\\d+}/locations")
	public ResponseEntity<List<Location>> getAccountLocations(@PathVariable Long id) {
		if (!accountRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(locationRepository.findByAccount_Id(id));
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
				.map(a -> new AccountResponse(a.getId(), a.getUsername()))
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
					return new AccountResponse(friend.getId(), friend.getUsername());
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
		}
		return new AccountResponse(target.getId(), target.getUsername());
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
		friendshipRepository.findByAccountA_IdAndAccountB_Id(minId, maxId)
				.ifPresent(friendshipRepository::delete);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Registers a new account (no authentication required).
	 *
	 * @param request username and plain password (validated)
	 * @return 201 with {@link AccountResponse}, 409 if username exists (including DB constraint races),
	 *         400 on validation failure
	 */
	@PostMapping({ "", "/" })
	public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
		String username = request.username().trim();
		if (accountRepository.existsByUsername(username)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
		}
		try {
			Account saved = accountRepository
					.save(new Account(null, username, passwordEncoder.encode(request.password())));
			return ResponseEntity.status(HttpStatus.CREATED).body(new AccountResponse(saved.getId(), saved.getUsername()));
		} catch (DataIntegrityViolationException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
		}
	}

	private Account requireAccount(Authentication authentication) {
		return accountRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}
}
