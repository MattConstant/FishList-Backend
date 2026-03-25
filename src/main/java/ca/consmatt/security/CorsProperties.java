package ca.consmatt.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * CORS for browser frontends on a different origin (Vite, CRA, Next dev server, etc.).
 */
@Data
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

	/**
	 * Allowed {@code Origin} patterns. Use {@code *} for local dev. For production, list explicit
	 * origins (e.g. {@code https://app.example.com}).
	 */
	private List<String> allowedOriginPatterns = new ArrayList<>(List.of("*"));

	/**
	 * If true, the browser may send cookies. You must then set {@link #allowedOriginPatterns} to
	 * explicit origins — not {@code *} (browsers reject that combination).
	 */
	private boolean allowCredentials = false;

	/** Cache duration for preflight {@code OPTIONS} responses (seconds). */
	private long maxAgeSeconds = 3600;
}
