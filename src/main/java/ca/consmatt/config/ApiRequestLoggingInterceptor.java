package ca.consmatt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Logs every API request as a single in/out pair so you can see who hit what,
 * with status code and duration. The format intentionally uses arrow markers
 * so the request flow is easy to scan in the docker logs:
 *
 * <pre>
 *   → POST /api/locations               user=alice
 *   ← POST /api/locations  201 (42 ms)  user=alice
 *   ! GET  /api/admin/me   403 (3 ms)   user=bob       (4xx warning)
 *   ✗ POST /api/auth/login 500 (8 ms)   user=anonymous (5xx / threw)
 * </pre>
 */
@Component
public class ApiRequestLoggingInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ApiRequestLoggingInterceptor.class);
	private static final String START_TIME_ATTRIBUTE = "requestStartTimeMs";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
		log.info("→ {} {}  user={}", request.getMethod(), request.getRequestURI(), currentUsername());
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		Object startValue = request.getAttribute(START_TIME_ATTRIBUTE);
		long durationMs = 0;
		if (startValue instanceof Long start) {
			durationMs = System.currentTimeMillis() - start;
		}
		int status = response.getStatus();
		String user = currentUsername();
		if (ex != null) {
			log.error("✗ {} {}  {} ({} ms)  user={}", request.getMethod(), request.getRequestURI(), status, durationMs,
					user, ex);
			return;
		}
		String marker = status >= 500 ? "✗" : status >= 400 ? "!" : "←";
		if (status >= 500) {
			log.error("{} {} {}  {} ({} ms)  user={}", marker, request.getMethod(), request.getRequestURI(), status,
					durationMs, user);
		} else if (status >= 400) {
			log.warn("{} {} {}  {} ({} ms)  user={}", marker, request.getMethod(), request.getRequestURI(), status,
					durationMs, user);
		} else {
			log.info("{} {} {}  {} ({} ms)  user={}", marker, request.getMethod(), request.getRequestURI(), status,
					durationMs, user);
		}
	}

	/** Returns the authenticated username, or {@code "anonymous"} when not signed in. */
	private static String currentUsername() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			return "anonymous";
		}
		String name = auth.getName();
		if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
			return "anonymous";
		}
		return name;
	}
}
