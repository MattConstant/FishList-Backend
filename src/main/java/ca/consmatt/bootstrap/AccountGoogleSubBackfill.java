package ca.consmatt.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time style backfill: pre-Google {@code account} rows may have {@code google_sub} null after Hibernate
 * adds the column. Assigns stable synthetic values so the column is populated before other runners query accounts.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccountGoogleSubBackfill implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(AccountGoogleSubBackfill.class);

	private final JdbcTemplate jdbcTemplate;

	@Value("${spring.datasource.url:}")
	private String jdbcUrl;

	public AccountGoogleSubBackfill(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(String... args) {
		if (jdbcUrl == null || !jdbcUrl.contains("postgresql")) {
			return;
		}
		try {
			int n = jdbcTemplate.update(
					"UPDATE account SET google_sub = 'legacy-migrated-' || id::text WHERE google_sub IS NULL");
			if (n > 0) {
				log.info("Backfilled google_sub for {} legacy account row(s)", n);
			}
		} catch (Exception e) {
			log.warn("Could not backfill google_sub (column may not exist yet): {}", e.getMessage());
		}
	}
}
