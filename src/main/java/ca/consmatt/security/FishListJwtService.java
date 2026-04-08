package ca.consmatt.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

/**
 * Creates signed FishList access tokens for use with {@code Authorization: Bearer}.
 */
@Service
@RequiredArgsConstructor
public class FishListJwtService {

	private final FishListJwtProperties properties;

	public String createAccessToken(String username) {
		if (properties.getSecret() == null || properties.getSecret().length() < 32) {
			throw new IllegalStateException("app.jwt.secret must be set to at least 32 characters");
		}
		SecretKey key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
		long now = System.currentTimeMillis();
		long exp = now + properties.getExpirationSeconds() * 1000L;
		return Jwts.builder()
				.subject(username)
				.claim("username", username)
				.issuedAt(new Date(now))
				.expiration(new Date(exp))
				.signWith(key)
				.compact();
	}
}
