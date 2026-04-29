package ca.consmatt.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ca.consmatt.service.AchievementService;
import lombok.RequiredArgsConstructor;

/**
 * Awards retroactive achievements on startup for any existing user whose past activity now meets
 * the criteria. Idempotent — re-runs do nothing once every eligible row exists.
 *
 * Without this, users who registered before the achievements feature shipped would have to log a
 * new catch (or generate any qualifying event) to earn badges they had already objectively earned.
 *
 * Runs after {@link EnumCheckConstraintMigration} so any newly-added enum codes are insertable.
 */
@Component
@Order(10)
@RequiredArgsConstructor
public class AchievementBackfillBootstrap implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(AchievementBackfillBootstrap.class);

	private final AchievementService achievementService;

	@Override
	@Transactional
	public void run(String... args) {
		int inserted = achievementService.backfillAll();
		if (inserted > 0) {
			log.info("ACHIEVEMENT_BACKFILL inserted {} retroactive unlock(s) on startup", inserted);
		}
	}
}
