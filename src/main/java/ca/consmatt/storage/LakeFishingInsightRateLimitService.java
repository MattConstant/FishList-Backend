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
 * In-memory per-user daily limiter for lake fishing insight requests.
 */
@Service
public class LakeFishingInsightRateLimitService {

	private record DailyCounter(LocalDate day, AtomicInteger count) {
	}

	private final ConcurrentHashMap<String, DailyCounter> counters = new ConcurrentHashMap<>();

	@Value("${openai.lake-insights.daily-limit:40}")
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
					"Daily lake insight limit reached (" + dailyLimit + " per day)");
		}
	}
}
