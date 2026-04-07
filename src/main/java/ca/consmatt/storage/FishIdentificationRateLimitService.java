package ca.consmatt.storage;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * In-memory per-user daily limiter for AI fish identification endpoint.
 */
@Service
public class FishIdentificationRateLimitService {

	private record DailyCounter(LocalDate day, AtomicInteger count) {
	}

	private final ConcurrentHashMap<String, DailyCounter> counters = new ConcurrentHashMap<>();

	@Value("${openai.identify.daily-limit:2}")
	private int dailyLimit;

	public void assertWithinDailyLimit(String username) {
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		DailyCounter updated = counters.compute(username, (key, existing) -> {
			if (existing == null || !existing.day().equals(today)) {
				return new DailyCounter(today, new AtomicInteger(1));
			}
			existing.count().incrementAndGet();
			return existing;
		});

		if (updated.count().get() > dailyLimit) {
			throw new ResponseStatusException(
					HttpStatus.TOO_MANY_REQUESTS,
					"Daily AI identification limit reached (" + dailyLimit + " per day)");
		}
	}
}
