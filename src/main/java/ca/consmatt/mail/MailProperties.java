package ca.consmatt.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FishList outbound mail and frontend links for verification emails.
 */
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

	/**
	 * When false, verification links are logged instead of emailed (local dev).
	 */
	private boolean enabled = false;

	private String from = "noreply@fishlist.ca";

	/** Public site URL used in confirmation links (no trailing slash). */
	private String frontendBaseUrl = "http://localhost:3000";

	private long verificationTtlHours = 24;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getFrontendBaseUrl() {
		return frontendBaseUrl;
	}

	public void setFrontendBaseUrl(String frontendBaseUrl) {
		this.frontendBaseUrl = frontendBaseUrl;
	}

	public long getVerificationTtlHours() {
		return verificationTtlHours;
	}

	public void setVerificationTtlHours(long verificationTtlHours) {
		this.verificationTtlHours = verificationTtlHours;
	}
}
