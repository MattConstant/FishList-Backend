package ca.consmatt.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * CORS for browser frontends on a different origin (Vite, Next, etc.). Configure explicit
 * {@link #allowedOriginPatterns} in {@code application.properties} or env — do not use a bare
 * {@code *} origin (rejected at startup; use e.g. {@code http://localhost:3000} for local dev).
 */
@Data
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

	/**
	 * Allowed {@code Origin} patterns (scheme + host + port), e.g. {@code https://app.example.com}.
	 * Subdomain wildcards like {@code https://*.example.com} are supported by Spring.
	 */
	private List<String> allowedOriginPatterns = new ArrayList<>();

	/**
	 * If true, the browser may send cookies. You must then set {@link #allowedOriginPatterns} to
	 * explicit origins — not {@code *} (browsers reject that combination).
	 */
	private boolean allowCredentials = false;

	/** Cache duration for preflight {@code OPTIONS} responses (seconds). */
	private long maxAgeSeconds = 3600;
}
