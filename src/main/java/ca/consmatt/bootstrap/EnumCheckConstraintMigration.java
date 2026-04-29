package ca.consmatt.bootstrap;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Drops stale {@code CHECK} constraints that Hibernate generates from JPA enum columns.
 *
 * Hibernate's {@code ddl-auto=update} creates a {@code CHECK (col in (...))} constraint the first
 * time it sees an {@link jakarta.persistence.Enumerated} column, but it never modifies that
 * constraint when the enum gains new values, so adding (e.g.) a new {@code AchievementCode}
 * causes inserts to fail with {@code violates check constraint &lt;table&gt;_&lt;col&gt;_check}.
 *
 * Each entity that uses an enum column now also sets {@code columnDefinition} so Hibernate skips
 * the auto-generated CHECK on fresh schemas; this runner only handles upgrade-in-place where the
 * old constraint already exists. Idempotent (uses {@code DROP CONSTRAINT IF EXISTS}); ordered
 * before any other startup runner that writes to these tables.
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class EnumCheckConstraintMigration implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(EnumCheckConstraintMigration.class);

	/** {@code (table, constraint)} pairs to drop. Constraint names follow the Hibernate default. */
	private static final String[][] CONSTRAINTS = {
		{ "account_achievements", "account_achievements_code_check" },
		{ "catches", "catches_fishing_type_check" },
		{ "location", "location_water_type_check" },
		{ "location", "location_visibility_check" },
		{ "account", "account_role_check" },
	};

	private final DataSource dataSource;

	@Override
	public void run(String... args) {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			for (String[] pair : CONSTRAINTS) {
				String sql = "ALTER TABLE " + pair[0] + " DROP CONSTRAINT IF EXISTS " + pair[1];
				try {
					stmt.executeUpdate(sql);
				} catch (SQLException ex) {
					// Log and continue — a missing table just means that entity isn't on this DB yet.
					log.warn("ENUM_CHECK_DROP skipped {} (reason: {})", pair[1], ex.getMessage());
				}
			}
			log.info("ENUM_CHECK_DROP completed");
		} catch (SQLException ex) {
			log.error("ENUM_CHECK_DROP migration failed; achievement inserts may still violate stale CHECK constraints", ex);
		}
	}
}
