package ca.consmatt.controllers;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ca.consmatt.beans.Account;
import ca.consmatt.beans.AccountRole;
import ca.consmatt.dto.AccountResponse;
import ca.consmatt.dto.GoogleAuthRequest;
import ca.consmatt.dto.GoogleAuthResponse;
import ca.consmatt.dto.PasswordLoginRequest;
import ca.consmatt.dto.RegisterRequest;
import ca.consmatt.policy.UsernamePolicy;
import ca.consmatt.repositories.AccountRepository;
import ca.consmatt.security.AdminProperties;
import ca.consmatt.security.FishListJwtService;
import ca.consmatt.security.GoogleIdTokenVerifierService;
import ca.consmatt.security.GoogleIdTokenVerifierService.GoogleIdTokenPayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Google Sign-In token exchange: validates Google ID token, returns FishList JWT.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final GoogleIdTokenVerifierService googleIdTokenVerifierService;
	private final AccountRepository accountRepository;
	private final AdminProperties adminProperties;
	private final FishListJwtService fishListJwtService;
	private final UsernamePolicy usernamePolicy;
	private final PasswordEncoder passwordEncoder;

	/**
	 * Username + password login. {@link Account#getPassword()} must hold a BCrypt hash (see dev seed data).
	 * Google-only accounts without a stored hash receive 401.
	 */
	@PostMapping("/login")
	public ResponseEntity<GoogleAuthResponse> login(@Valid @RequestBody PasswordLoginRequest request) {
		String u = request.username().trim();
		if (u.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username required");
		}
		Account account = accountRepository.findByUsername(u)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
		String hash = account.getPassword();
		if (hash == null || hash.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
		}
		if (!passwordEncoder.matches(request.password(), hash)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
		}
		String accessToken = fishListJwtService.createAccessToken(account.getUsername());
		return ResponseEntity.ok(new GoogleAuthResponse(accessToken, "Bearer",
				new AccountResponse(account.getId(), account.getUsername(), account.getProfileImageKey())));
	}

	/**
	 * Create a password-backed account (no Google link). Username rules match profile updates.
	 */
	@PostMapping("/register")
	public ResponseEntity<GoogleAuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		String u = request.username().trim();
		if (u.length() < 2 || u.length() > 100) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Username must be between 2 and 100 characters");
		}
		if (!u.matches("^[a-zA-Z0-9_]{2,100}$")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Username may only contain letters, numbers, and underscores (2–100 characters)");
		}
		usernamePolicy.validateProposedUsername(u, null);
		if (accountRepository.existsByUsername(u)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already taken");
		}
		String hash = passwordEncoder.encode(request.password());
		try {
			Account saved = accountRepository.save(new Account(null, u, null, hash, AccountRole.USER, null));
			String accessToken = fishListJwtService.createAccessToken(saved.getUsername());
			return ResponseEntity.ok(new GoogleAuthResponse(accessToken, "Bearer",
					new AccountResponse(saved.getId(), saved.getUsername(), saved.getProfileImageKey())));
		} catch (DataIntegrityViolationException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already taken");
		}
	}

	@PostMapping("/google")
	public ResponseEntity<GoogleAuthResponse> google(@Valid @RequestBody GoogleAuthRequest request) {
		if (googleIdTokenVerifierService.credentialsNotConfigured()) {
			throw new ResponseStatusException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"Google OAuth is not configured (set GOOGLE_CLIENT_ID / app.oauth2.google.client-id)");
		}
		GoogleIdTokenPayload payload = googleIdTokenVerifierService.verify(request.credential())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google credential"));

		Account account = accountRepository.findByGoogleSub(payload.sub())
				.orElseGet(() -> createAccountFromGoogle(payload));

		String accessToken = fishListJwtService.createAccessToken(account.getUsername());
		return ResponseEntity.ok(new GoogleAuthResponse(accessToken, "Bearer",
				new AccountResponse(account.getId(), account.getUsername(), account.getProfileImageKey())));
	}

	private Account createAccountFromGoogle(GoogleIdTokenPayload payload) {
		String username = deriveUniqueUsername(payload.email(), payload.sub());
		AccountRole role = adminProperties.isAdminUsername(username) ? AccountRole.ADMIN : AccountRole.USER;
		try {
			return accountRepository.save(new Account(null, username, payload.sub(), null, role, null));
		} catch (DataIntegrityViolationException e) {
			return accountRepository.findByGoogleSub(payload.sub())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Could not create account"));
		}
	}

	private String deriveUniqueUsername(String email, String googleSub) {
		String base;
		if (email != null && !email.isBlank() && email.contains("@")) {
			String local = email.substring(0, email.indexOf('@'));
			base = local.replaceAll("[^a-zA-Z0-9_]", "_");
			if (base.isEmpty()) {
				base = "user";
			}
		} else {
			base = "google_" + googleSub.substring(0, Math.min(8, googleSub.length())).replaceAll("[^a-zA-Z0-9_]", "_");
			if (base.equals("google_")) {
				base = "user";
			}
		}
		if (base.length() > 100) {
			base = base.substring(0, 100);
		}
		String candidate = base;
		int n = 0;
		while (accountRepository.existsByUsername(candidate) || !usernamePolicy.isAcceptableNewAccountUsername(candidate)) {
			n++;
			if (n > 5000) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not allocate a username");
			}
			String suffix = "_" + n;
			int maxBase = Math.max(1, 100 - suffix.length());
			candidate = base.substring(0, Math.min(base.length(), maxBase)) + suffix;
		}
		return candidate;
	}
}
