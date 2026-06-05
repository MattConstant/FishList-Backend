package ca.consmatt.mail;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.consmatt.beans.Account;
import ca.consmatt.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(MailProperties.class)
public class EmailVerificationService {

	private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

	private final AccountRepository accountRepository;
	private final MailProperties mailProperties;
	private final Optional<JavaMailSender> mailSender;

	public String normalizeEmail(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.trim().toLowerCase();
	}

	public void issueVerificationToken(Account account) {
		String token = UUID.randomUUID().toString().replace("-", "");
		Instant expires = Instant.now().plus(mailProperties.getVerificationTtlHours(), ChronoUnit.HOURS);
		account.setEmailVerificationToken(token);
		account.setEmailVerificationExpiresAt(expires);
	}

	@Transactional
	public void sendVerificationEmail(Account account) {
		String token = account.getEmailVerificationToken();
		if (token == null || token.isBlank()) {
			issueVerificationToken(account);
			accountRepository.save(account);
			token = account.getEmailVerificationToken();
		}
		String link = buildVerificationLink(token);
		String to = account.getEmail();
		if (to == null || to.isBlank()) {
			log.warn("EMAIL_VERIFY_SKIP account={} reason=no_email", account.getUsername());
			return;
		}

		if (!mailProperties.isEnabled() || mailSender.isEmpty()) {
			log.info("EMAIL_VERIFY_LINK user={} email={} url={}", account.getUsername(), to, link);
			return;
		}

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(mailProperties.getFrom());
		message.setTo(to);
		message.setSubject("Confirm your FishList account");
		message.setText("""
				Hi %s,

				Thanks for signing up for FishList. Confirm your email to finish creating your account:

				%s

				This link expires in %d hours. If you did not create an account, you can ignore this email.
				""".formatted(account.getUsername(), link, mailProperties.getVerificationTtlHours()));
		try {
			mailSender.get().send(message);
			log.info("EMAIL_VERIFY_SENT user={} email={}", account.getUsername(), to);
		} catch (MailException e) {
			log.error("EMAIL_VERIFY_FAILED user={} email={}", account.getUsername(), to, e);
			throw e;
		}
	}

	public String buildVerificationLink(String token) {
		String base = mailProperties.getFrontendBaseUrl().replaceAll("/+$", "");
		return base + "/verify-email?token=" + token;
	}

	@Transactional
	public boolean verifyToken(String token) {
		if (token == null || token.isBlank()) {
			return false;
		}
		Account account = accountRepository.findByEmailVerificationToken(token.trim()).orElse(null);
		if (account == null) {
			return false;
		}
		Instant expires = account.getEmailVerificationExpiresAt();
		if (expires == null || expires.isBefore(Instant.now())) {
			return false;
		}
		account.setEmailVerified(true);
		account.setEmailVerificationToken(null);
		account.setEmailVerificationExpiresAt(null);
		accountRepository.save(account);
		log.info("EMAIL_VERIFIED user={} email={}", account.getUsername(), account.getEmail());
		return true;
	}

	@Transactional
	public void resendForEmail(String email) {
		String normalized = normalizeEmail(email);
		if (normalized.isEmpty()) {
			return;
		}
		Account account = accountRepository.findByEmail(normalized).orElse(null);
		if (account == null || account.isEmailVerified()) {
			return;
		}
		issueVerificationToken(account);
		accountRepository.save(account);
		sendVerificationEmail(account);
	}
}
