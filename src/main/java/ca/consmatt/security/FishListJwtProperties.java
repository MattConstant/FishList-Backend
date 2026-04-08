package ca.consmatt.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * HMAC signing key and TTL for FishList-issued API JWTs (not Google tokens).
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class FishListJwtProperties {

	/**
	 * HMAC-SHA256 key material; must be at least 256 bits (32 bytes) for {@link io.jsonwebtoken.security.Keys}.
	 */
	private String secret = "";

	/** Access token lifetime in seconds (default 24h). */
	private long expirationSeconds = 86400;
}
