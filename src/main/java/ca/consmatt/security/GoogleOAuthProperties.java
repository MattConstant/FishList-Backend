package ca.consmatt.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Google OAuth client ID used to validate ID tokens (must match the web client used in the browser).
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.oauth2.google")
public class GoogleOAuthProperties {

	private String clientId = "";
}
