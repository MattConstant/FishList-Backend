package ca.consmatt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Logs API handler start/end and response status for backend function tracing.
 */
@Component
public class ApiRequestLoggingInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ApiRequestLoggingInterceptor.class);
	private static final String START_TIME_ATTRIBUTE = "requestStartTimeMs";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
		log.info("START {} {}", request.getMethod(), request.getRequestURI());
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		Object startValue = request.getAttribute(START_TIME_ATTRIBUTE);
		long durationMs = 0;
		if (startValue instanceof Long start) {
			durationMs = System.currentTimeMillis() - start;
		}
		if (ex != null) {
			log.error("END {} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs,
					ex);
			return;
		}
		log.info("END {} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
	}
}
