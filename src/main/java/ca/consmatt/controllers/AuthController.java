package ca.consmatt.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;
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
import ca.consmatt.dto.RegisterResponse;
import ca.consmatt.dto.ResendVerificationRequest;
import ca.consmatt.dto.VerifyEmailRequest;
import ca.consmatt.mail.EmailVerificationService;
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

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final GoogleIdTokenVerifierService googleIdTokenVerifierService;
	private final AccountRepository accountRepository;
	private final AdminProperties adminProperties;
	private final FishListJwtService fishListJwtService;
	private final UsernamePolicy usernamePolicy;
	private final PasswordEncoder passwordEncoder;
	private final EmailVerificationService emailVerificationService;

	/**
	 * Username + password login. {@link Account#getPassword()} must hold a BCrypt hash (see dev seed data).
	 * Google-only accounts without a stored hash receive 401.
	 */
	@PostMapping("/login")
	public ResponseEntity<GoogleAuthResponse> login(@Valid @RequestBody PasswordLoginRequest request) {
		String u = request.username().trim();
		if (u.isEmpty()) {
			log.warn("LOGIN_FAILED  user=<empty> reason=missing_username");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username required");
		}
		Account account = accountRepository.findByUsername(u).orElse(null);
		if (account == null) {
			log.warn("LOGIN_FAILED  user={} reason=unknown_user", u);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
		}
		String hash = account.getPassword();
		if (hash == null || hash.isBlank()) {
			log.warn("LOGIN_FAILED  user={} reason=no_password_set", u);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
		}
		if (!passwordEncoder.matches(request.password(), hash)) {
			log.warn("LOGIN_FAILED  user={} reason=bad_password", u);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
		}
		if (!account.isEmailVerified()) {
			log.warn("LOGIN_FAILED  user={} reason=email_not_verified", u);
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"EMAIL_NOT_VERIFIED:Confirm your email before signing in. Check your inbox or request a new link.");
		}
		String accessToken = fishListJwtService.createAccessToken(account.getUsername());
		log.info("LOGIN_SUCCESS user={} method=password role={}", account.getUsername(), account.getRole());
		return ResponseEntity.ok(new GoogleAuthResponse(accessToken, "Bearer",
				new AccountResponse(account.getId(), account.getUsername(), account.getProfileImageKey())));
	}

	/**
	 * Create a password-backed account (no Google link). Username rules match profile updates.
	 * Sends a confirmation email; no JWT until the address is verified.
	 */
	@PostMapping("/register")
	public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
		String u = request.username().trim();
		if (u.length() < 2 || u.length() > 100) {
			log.warn("REGISTER_FAILED user={} reason=length", u);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Username must be between 2 and 100 characters");
		}
		if (!u.matches("^[a-zA-Z0-9_]{2,100}$")) {
			log.warn("REGISTER_FAILED user={} reason=invalid_chars", u);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Username may only contain letters, numbers, and underscores (2–100 characters)");
		}
		usernamePolicy.validateProposedUsername(u, null);
		if (accountRepository.existsByUsername(u)) {
			log.warn("REGISTER_FAILED user={} reason=already_taken", u);
			throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already taken");
		}
		String email = emailVerificationService.normalizeEmail(request.email());
		if (email.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid email is required");
		}
		if (accountRepository.existsByEmail(email)) {
			log.warn("REGISTER_FAILED user={} reason=email_taken email={}", u, email);
			throw new ResponseStatusException(HttpStatus.CONFLICT, "That email is already registered");
		}
		String hash = passwordEncoder.encode(request.password());
		try {
			Account saved = new Account(null, u, null, hash, AccountRole.USER, null, email, false, null, null);
			emailVerificationService.issueVerificationToken(saved);
			accountRepository.save(saved);
			try {
				emailVerificationService.sendVerificationEmail(saved);
			} catch (MailException e) {
				log.error("REGISTER_EMAIL_FAILED user={} email={}", saved.getUsername(), email, e);
			}
			log.info("REGISTERED    user={} id={} email={} pending_verification=true", saved.getUsername(),
					saved.getId(), email);
			return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse(
					"Check your email to confirm your account before signing in.",
					email,
					true));
		} catch (DataIntegrityViolationException e) {
			log.warn("REGISTER_FAILED user={} reason=race_already_taken", u);
			throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already taken");
		}
	}

	@PostMapping("/verify-email")
	public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		boolean ok = emailVerificationService.verifyToken(request.token());
		if (!ok) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"This confirmation link is invalid or has expired.");
		}
		return ResponseEntity.ok(Map.of("message", "Email confirmed. You can sign in now."));
	}

	@PostMapping("/resend-verification")
	public ResponseEntity<Map<String, String>> resendVerification(
			@Valid @RequestBody ResendVerificationRequest request) {
		emailVerificationService.resendForEmail(request.email());
		return ResponseEntity.ok(Map.of(
				"message",
				"If an unverified account exists for that email, we sent a new confirmation link."));
	}

	@PostMapping("/google")
	public ResponseEntity<GoogleAuthResponse> google(@Valid @RequestBody GoogleAuthRequest request) {
		if (googleIdTokenVerifierService.credentialsNotConfigured()) {
			log.warn("LOGIN_FAILED  method=google reason=not_configured");
			throw new ResponseStatusException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"Google OAuth is not configured (set GOOGLE_CLIENT_ID / app.oauth2.google.client-id)");
		}
		GoogleIdTokenPayload payload = googleIdTokenVerifierService.verify(request.credential())
				.orElseThrow(() -> {
					log.warn("LOGIN_FAILED  method=google reason=invalid_credential");
					return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google credential");
				});

		boolean[] createdHolder = { false };
		Account account = accountRepository.findByGoogleSub(payload.sub())
				.orElseGet(() -> {
					createdHolder[0] = true;
					return createAccountFromGoogle(payload);
				});

		String accessToken = fishListJwtService.createAccessToken(account.getUsername());
		log.info("LOGIN_SUCCESS user={} method=google new_account={} role={}", account.getUsername(),
				createdHolder[0], account.getRole());
		return ResponseEntity.ok(new GoogleAuthResponse(accessToken, "Bearer",
				new AccountResponse(account.getId(), account.getUsername(), account.getProfileImageKey())));
	}

	private Account createAccountFromGoogle(GoogleIdTokenPayload payload) {
		String username = deriveUniqueUsername(payload.email(), payload.sub());
		AccountRole role = adminProperties.isAdminUsername(username) ? AccountRole.ADMIN : AccountRole.USER;
		String email = emailVerificationService.normalizeEmail(payload.email());
		try {
			return accountRepository.save(new Account(null, username, payload.sub(), null, role, null, email, true,
					null, null));
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
