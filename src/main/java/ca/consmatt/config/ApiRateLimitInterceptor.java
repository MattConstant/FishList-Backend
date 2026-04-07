package ca.consmatt.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lightweight in-memory rate limiter for high-cost API endpoints.
 */
@Component
public class ApiRateLimitInterceptor implements HandlerInterceptor {

	private record WindowCounter(long windowStartMs, AtomicInteger count) {
	}

	private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

	@Value("${rate-limit.feed.per-minute:60}")
	private int feedPerMinute;

	@Value("${rate-limit.search.per-minute:40}")
	private int searchPerMinute;

	@Value("${rate-limit.storage-download-url.per-minute:60}")
	private int storageDownloadUrlPerMinute;

	@Value("${rate-limit.location-writes.per-minute:45}")
	private int locationWritesPerMinute;

	@Value("${rate-limit.interactions.per-minute:120}")
	private int interactionsPerMinute;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String method = request.getMethod();
		String path = request.getRequestURI();
		String principal = resolvePrincipal(request);

		if ("GET".equals(method) && path.startsWith("/api/locations/feed")) {
			enforce("feed", principal, feedPerMinute, 60_000L);
		}
		if ("GET".equals(method) && path.startsWith("/api/accounts/search")) {
			enforce("search", principal, searchPerMinute, 60_000L);
		}
		if ("GET".equals(method) && path.startsWith("/api/storage/images/download-url")) {
			enforce("storage-download-url", principal, storageDownloadUrlPerMinute, 60_000L);
		}
		if (("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method))
				&& path.startsWith("/api/locations")) {
			enforce("location-writes", principal, locationWritesPerMinute, 60_000L);
		}
		if (("POST".equals(method) || "DELETE".equals(method))
				&& (path.contains("/comments") || path.endsWith("/like"))) {
			enforce("interactions", principal, interactionsPerMinute, 60_000L);
		}

		return true;
	}

	private void enforce(String bucket, String principal, int maxRequests, long windowMs) {
		long now = System.currentTimeMillis();
		String key = bucket + ":" + principal;
		WindowCounter counter = counters.compute(key, (k, existing) -> {
			if (existing == null || now - existing.windowStartMs() >= windowMs) {
				return new WindowCounter(now, new AtomicInteger(1));
			}
			existing.count().incrementAndGet();
			return existing;
		});
		if (counter.count().get() > maxRequests) {
			throw new ResponseStatusException(
					HttpStatus.TOO_MANY_REQUESTS,
					"Rate limit exceeded for " + bucket + ". Try again in about a minute.");
		}
	}

	private String resolvePrincipal(HttpServletRequest request) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && auth.getName() != null && !auth.getName().isBlank()) {
			return auth.getName();
		}
		String remote = request.getRemoteAddr();
		return remote == null || remote.isBlank() ? "anonymous" : "ip:" + remote;
	}
}
