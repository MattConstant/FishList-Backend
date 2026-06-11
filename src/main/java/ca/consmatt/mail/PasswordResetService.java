package ca.consmatt.mail;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.consmatt.beans.Account;
import ca.consmatt.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(MailProperties.class)
public class PasswordResetService {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

	private final AccountRepository accountRepository;
	private final MailProperties mailProperties;
	private final Optional<JavaMailSender> mailSender;
	private final EmailVerificationService emailVerificationService;

	public String maskEmail(String email) {
		if (email == null || email.isBlank()) {
			return null;
		}
		int at = email.indexOf('@');
		if (at <= 0) {
			return "***";
		}
		String local = email.substring(0, at);
		String domain = email.substring(at);
		if (local.length() == 1) {
			return local.charAt(0) + "***" + domain;
		}
		return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
	}

	public boolean hasRegisteredEmail(Account account) {
		return account.getEmail() != null && !account.getEmail().isBlank();
	}

	@Transactional
	public void issueResetToken(Account account) {
		String token = UUID.randomUUID().toString().replace("-", "");
		Instant expires = Instant.now().plus(mailProperties.getPasswordResetTtlHours(), ChronoUnit.HOURS);
		account.setPasswordResetToken(token);
		account.setPasswordResetExpiresAt(expires);
	}

	@Transactional
	public void sendResetEmail(Account account) {
		if (!hasRegisteredEmail(account)) {
			log.warn("PASSWORD_RESET_SKIP account={} reason=no_email", account.getUsername());
			return;
		}
		issueResetToken(account);
		accountRepository.save(account);
		sendResetEmailWithExistingToken(account);
	}

	private void sendResetEmailWithExistingToken(Account account) {
		String token = account.getPasswordResetToken();
		if (token == null || token.isBlank()) {
			return;
		}
		String link = buildResetLink(token);
		String to = account.getEmail();

		if (!mailProperties.isEnabled() || mailSender.isEmpty()) {
			log.info("PASSWORD_RESET_LINK user={} email={} url={}", account.getUsername(), to, link);
			return;
		}

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(mailProperties.getFrom());
		message.setTo(to);
		message.setSubject("Reset your FishList password");
		message.setText("""
				Hi %s,

				We received a request to reset your FishList password. Choose a new password here:

				%s

				This link expires in %d hour(s). If you did not request this, you can ignore this email.
				""".formatted(account.getUsername(), link, mailProperties.getPasswordResetTtlHours()));
		try {
			mailSender.get().send(message);
			log.info("PASSWORD_RESET_SENT user={} email={}", account.getUsername(), to);
		} catch (MailException e) {
			log.error("PASSWORD_RESET_FAILED user={} email={}", account.getUsername(), to, e);
			throw e;
		}
	}

	public String buildResetLink(String token) {
		String base = mailProperties.getFrontendBaseUrl().replaceAll("/+$", "");
		return base + "/reset-password?token=" + token;
	}

	@Transactional
	public void requestResetForEmail(String rawEmail) {
		String normalized = emailVerificationService.normalizeEmail(rawEmail);
		if (normalized.isEmpty()) {
			return;
		}
		Account account = accountRepository.findByEmail(normalized).orElse(null);
		if (account == null || !hasRegisteredEmail(account)) {
			return;
		}
		sendResetEmail(account);
	}

	@Transactional
	public Account consumeResetToken(String token, String encodedPassword) {
		if (token == null || token.isBlank()) {
			return null;
		}
		Account account = accountRepository.findByPasswordResetToken(token.trim()).orElse(null);
		if (account == null) {
			return null;
		}
		Instant expires = account.getPasswordResetExpiresAt();
		if (expires == null || expires.isBefore(Instant.now())) {
			return null;
		}
		account.setPassword(encodedPassword);
		account.setPasswordResetToken(null);
		account.setPasswordResetExpiresAt(null);
		return accountRepository.save(account);
	}
}
