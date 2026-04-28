package ca.consmatt.bootstrap;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ca.consmatt.beans.Location;
import ca.consmatt.repositories.CatchCommentRepository;
import ca.consmatt.repositories.CatchLikeRepository;
import ca.consmatt.repositories.CatchRepository;
import ca.consmatt.repositories.LocationRepository;
import lombok.RequiredArgsConstructor;

/**
 * Locations only make sense as a container for catches. If catches were deleted before
 * we added auto-pruning to {@code LocationController.deleteCatch}, the empty location pin
 * was left behind on the user's profile. This sweep removes any zero-catch locations on startup
 * (idempotent — does nothing once the data is clean).
 */
@Component
@RequiredArgsConstructor
public class EmptyLocationCleanupBootstrap implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(EmptyLocationCleanupBootstrap.class);

	private final LocationRepository locationRepository;
	private final CatchRepository catchRepository;
	private final CatchLikeRepository catchLikeRepository;
	private final CatchCommentRepository catchCommentRepository;

	@Override
	@Transactional
	public void run(String... args) {
		List<Location> all = locationRepository.findAll();
		int removed = 0;
		for (Location loc : all) {
			Long id = loc.getId();
			if (id == null) {
				continue;
			}
			if (catchRepository.countByLocation_Id(id) == 0) {
				catchCommentRepository.deleteByCatchRecord_Location_Id(id);
				catchLikeRepository.deleteByCatchRecord_Location_Id(id);
				locationRepository.deleteById(id);
				removed++;
			}
		}
		if (removed > 0) {
			log.info("LOCATION_CLEANUP removed {} empty location(s) on startup", removed);
		}
	}
}
