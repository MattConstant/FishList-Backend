package ca.consmatt.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configurable content rules layered on top of built-in defaults (see {@link ca.consmatt.policy.UsernamePolicy}).
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.content")
public class ContentPolicyProperties {

	/**
	 * Extra case-insensitive substring checks for usernames (merged with built-in defaults).
	 */
	private List<String> bannedUsernameSubstrings = new ArrayList<>();
}
