package ca.consmatt.controllers;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import ca.consmatt.beans.Account;
import ca.consmatt.beans.Location;
import ca.consmatt.dto.AccountResponse;
import ca.consmatt.dto.CreateAccountRequest;
import ca.consmatt.repositories.AccountRepository;
import ca.consmatt.repositories.LocationRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API for user accounts: registration, profile lookup, and a user's locations.
 */
@RestController
@RequestMapping("/api/accounts")
@CrossOrigin
@RequiredArgsConstructor
public class AccountController {

	private final AccountRepository accountRepository;
	private final LocationRepository locationRepository;
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
	 * Registers a new account (no authentication required).
	 *
	 * @param request username and plain password (validated)
	 * @return 201 with {@link AccountResponse}, 409 if username exists (including DB constraint races),
	 *         400 on validation failure
	 */
	@PostMapping({ "", "/" })
	public ResponseEntity<?> createAccount(@Valid @RequestBody CreateAccountRequest request) {
		String username = request.username().trim();
		if (accountRepository.existsByUsername(username)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already taken");
		}
		try {
			Account saved = accountRepository
					.save(new Account(null, username, passwordEncoder.encode(request.password())));
			return ResponseEntity.status(HttpStatus.CREATED).body(new AccountResponse(saved.getId(), saved.getUsername()));
		} catch (DataIntegrityViolationException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already taken");
		}
	}
}
