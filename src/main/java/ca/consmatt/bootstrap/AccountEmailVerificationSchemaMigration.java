package ca.consmatt.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Safe Postgres migration for email verification columns on existing {@code account} rows.
 * Hibernate {@code ddl-auto=update} cannot add {@code email_verified NOT NULL} when data exists;
 * this runs before other account queries on startup.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccountEmailVerificationSchemaMigration implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(AccountEmailVerificationSchemaMigration.class);

	private final JdbcTemplate jdbcTemplate;

	@Value("${spring.datasource.url:}")
	private String jdbcUrl;

	public AccountEmailVerificationSchemaMigration(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(String... args) {
		if (jdbcUrl == null || !jdbcUrl.contains("postgresql")) {
			return;
		}
		try {
			jdbcTemplate.execute("ALTER TABLE account ADD COLUMN IF NOT EXISTS email VARCHAR(255)");
			jdbcTemplate.execute(
					"ALTER TABLE account ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(64)");
			jdbcTemplate.execute(
					"ALTER TABLE account ADD COLUMN IF NOT EXISTS email_verification_expires_at TIMESTAMPTZ");
			jdbcTemplate.execute("ALTER TABLE account ADD COLUMN IF NOT EXISTS email_verified BOOLEAN");
			int backfilled = jdbcTemplate.update(
					"UPDATE account SET email_verified = TRUE WHERE email_verified IS NULL");
			jdbcTemplate.execute("ALTER TABLE account ALTER COLUMN email_verified SET DEFAULT TRUE");
			try {
				jdbcTemplate.execute("ALTER TABLE account ALTER COLUMN email_verified SET NOT NULL");
			} catch (Exception e) {
				log.warn("Could not set account.email_verified NOT NULL yet: {}", e.getMessage());
			}
			jdbcTemplate.execute(
					"CREATE UNIQUE INDEX IF NOT EXISTS uk_account_email ON account (email) WHERE email IS NOT NULL");
			if (backfilled > 0) {
				log.info("Backfilled email_verified=true for {} legacy account row(s)", backfilled);
			}
		} catch (Exception e) {
			log.warn("Account email verification schema migration skipped: {}", e.getMessage());
		}
	}
}
