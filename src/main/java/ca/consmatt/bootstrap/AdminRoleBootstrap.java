package ca.consmatt.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ca.consmatt.beans.AccountRole;
import ca.consmatt.repositories.AccountRepository;
import ca.consmatt.security.AdminProperties;
import lombok.RequiredArgsConstructor;

/**
 * Ensures configured admin usernames have ADMIN role on startup.
 */
@Component
@RequiredArgsConstructor
public class AdminRoleBootstrap implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminRoleBootstrap.class);

	private final AccountRepository accountRepository;
	private final AdminProperties adminProperties;

	@Override
	@Transactional
	public void run(String... args) {
		for (String username : adminProperties.getUsernames()) {
			if (username == null || username.isBlank()) {
				continue;
			}
			accountRepository.findByUsername(username.trim())
					.ifPresent(account -> {
						if (account.getRole() != AccountRole.ADMIN) {
							account.setRole(AccountRole.ADMIN);
							accountRepository.save(account);
							log.info("Promoted configured admin account: {}", account.getUsername());
						}
					});
		}
	}
}
