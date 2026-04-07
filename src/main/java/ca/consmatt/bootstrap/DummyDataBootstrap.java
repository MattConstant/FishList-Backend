package ca.consmatt.bootstrap;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ca.consmatt.beans.Account;
import ca.consmatt.beans.AccountRole;
import ca.consmatt.beans.Catch;
import ca.consmatt.beans.Condition;
import ca.consmatt.beans.Fish;
import ca.consmatt.beans.Location;
import ca.consmatt.repositories.AccountRepository;
import ca.consmatt.repositories.CatchRepository;
import ca.consmatt.repositories.ConditionRepository;
import ca.consmatt.repositories.FishRepository;
import ca.consmatt.repositories.LocationRepository;
import lombok.RequiredArgsConstructor;

/**
 * Seeds sample accounts, species, conditions, locations, and catches when the database has no
 * accounts (typically first H2 in-memory startup). Disabled when {@code prod} is active so
 * deployed instances do not pay this cost at startup (or seed production DBs by accident).
 */
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DummyDataBootstrap implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DummyDataBootstrap.class);

	private final AccountRepository accountRepository;
	private final LocationRepository locationRepository;
	private final FishRepository fishRepository;
	private final ConditionRepository conditionRepository;
	private final CatchRepository catchRepository;
	private final PasswordEncoder passwordEncoder;

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public void run(String... args) {
		if (accountRepository.count() > 0) {
			return;
		}

		try {
			loadDummyData();
		} catch (RuntimeException e) {
			log.error("Dummy data bootstrap failed", e);
			throw e;
		}
	}

	private void loadDummyData() {
		String plainPassword = System.getenv("FISHLIST_DUMMY_PASSWORD");
		if (plainPassword == null || plainPassword.isBlank()) {
			plainPassword = "DevOnly-" + UUID.randomUUID() + "-A1!";
			log.warn(
					"FISHLIST_DUMMY_PASSWORD not set. Generated one-time dummy password for seeded users: {}",
					plainPassword);
		}
		String encoded = passwordEncoder.encode(plainPassword);

		Account user = accountRepository.save(new Account(null, "user", encoded, AccountRole.USER));
		Account alice = accountRepository.save(new Account(null, "alice", encoded, AccountRole.USER));
		Account bob = accountRepository.save(new Account(null, "bob", encoded, AccountRole.USER));

		fishRepository.save(Fish.builder()
				.species("Brook trout")
				.averageWeight(0.5)
				.averageLength(25.0)
				.description("Native char; small streams.")
				.build());
		fishRepository.save(Fish.builder()
				.species("Northern pike")
				.averageWeight(2.0)
				.averageLength(60.0)
				.description("Aggressive predator.")
				.build());
		fishRepository.save(Fish.builder()
				.species("Largemouth bass")
				.averageWeight(1.0)
				.averageLength(30.0)
				.description("Popular sport fish.")
				.build());

		conditionRepository.save(Condition.builder()
				.weather("Sunny")
				.temperature(18.0)
				.windSpeed(12.0)
				.timeOfDay("Morning")
				.waterTemperature(16.0)
				.waterClarity("Clear")
				.pressure(1013.0)
				.moonPhase("New")
				.notes("Stable barometer.")
				.build());
		conditionRepository.save(Condition.builder()
				.weather("Overcast")
				.temperature(14.0)
				.windSpeed(8.0)
				.timeOfDay("Afternoon")
				.waterTemperature(15.0)
				.waterClarity("Stained")
				.pressure(1008.0)
				.moonPhase("Full")
				.notes("Muddy water after rain.")
				.build());

		Location lake = new Location();
		lake.setLocationName("Lake Clearwater");
		lake.setLatitude("45.1234");
		lake.setLongitude("-75.5678");
		lake.setTimeStamp("2025-06-01T08:00:00");
		lake.setDetails("Public dock; parking nearby.");
		lake.setAccount(user);
		lake = locationRepository.save(lake);

		Location river = new Location();
		river.setLocationName("River Bend");
		river.setLatitude("45.2000");
		river.setLongitude("-75.4000");
		river.setTimeStamp("2025-06-03T15:30:00");
		river.setDetails("Wade access only.");
		river.setAccount(alice);
		river = locationRepository.save(river);

		Location pond = new Location();
		pond.setLocationName("Hidden Pond");
		pond.setLatitude("45.0500");
		pond.setLongitude("-75.6200");
		pond.setTimeStamp("2025-05-20T12:00:00");
		pond.setDetails("Small pond; catch and release.");
		pond.setAccount(bob);
		pond = locationRepository.save(pond);

		catchRepository.save(Catch.builder()
				.location(lake)
				.species("Brook trout")
				.quantity(2)
				.lengthCm(28.0)
				.weightKg(0.45)
				.notes("Bright red spots.")
				.description("Caught on small spinner.")
				.build());
		catchRepository.save(Catch.builder()
				.location(lake)
				.species("Largemouth bass")
				.quantity(1)
				.lengthCm(32.0)
				.weightKg(0.85)
				.notes("Released after photo.")
				.build());
		catchRepository.save(Catch.builder()
				.location(river)
				.species("Northern pike")
				.quantity(1)
				.lengthCm(55.0)
				.weightKg(1.2)
				.notes("Jack pike, fun fight.")
				.build());
	}

}
