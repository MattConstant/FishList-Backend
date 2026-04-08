package ca.consmatt.security;

import java.util.Collections;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import lombok.RequiredArgsConstructor;

/**
 * Validates Google ID tokens from the GIS client.
 */
@Service
@RequiredArgsConstructor
public class GoogleIdTokenVerifierService {

	private final GoogleOAuthProperties properties;

	public Optional<GoogleIdTokenPayload> verify(String credentialJwt) {
		if (credentialsNotConfigured()) {
			return Optional.empty();
		}
		try {
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
					new NetHttpTransport(),
					GsonFactory.getDefaultInstance())
					.setAudience(Collections.singletonList(properties.getClientId().trim()))
					.build();
			GoogleIdToken idToken = verifier.verify(credentialJwt);
			if (idToken == null) {
				return Optional.empty();
			}
			com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = idToken.getPayload();
			String sub = payload.getSubject();
			String email = payload.getEmail();
			if (sub == null || sub.isBlank()) {
				return Optional.empty();
			}
			return Optional.of(new GoogleIdTokenPayload(sub, email != null ? email : ""));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public boolean credentialsNotConfigured() {
		return properties.getClientId() == null || properties.getClientId().isBlank();
	}

	public record GoogleIdTokenPayload(String sub, String email) {
	}
}
