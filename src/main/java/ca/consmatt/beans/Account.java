package ca.consmatt.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Application user. Google Sign-In sets {@link #googleSub}; legacy rows may have null until backfilled.
 * {@link #password} is unused for Google-only auth.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NonNull
	private String username;

	/**
	 * Google subject ({@code sub}). Nullable only for pre-migration DB rows; new sign-ups always set this.
	 */
	@Column(name = "google_sub", nullable = true, unique = true, length = 255)
	private String googleSub;

	/**
	 * BCrypt password hash for username/password login; {@code null} means Google-only (or not set).
	 */
	@JsonIgnore
	private String password;

	/**
	 * {@code columnDefinition} keeps Hibernate from auto-adding a {@code CHECK (role in (...))}
	 * constraint that would block future {@link AccountRole} additions.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "varchar(32) not null")
	private AccountRole role = AccountRole.USER;

	/**
	 * S3/MinIO object key under {@code uploads/{username}/…}; used for profile photo.
	 */
	@Column(name = "profile_image_key", length = 512)
	private String profileImageKey;

	/** Lowercase email for password sign-ups; nullable for legacy and Google-only rows. */
	@Column(nullable = true, unique = true, length = 255)
	private String email;

	/** Legacy and Google accounts default to verified; password sign-ups start false until confirmed. */
	@Column(name = "email_verified", nullable = false, columnDefinition = "boolean default true")
	private boolean emailVerified = true;

	@Column(name = "email_verification_token", nullable = true, length = 64)
	private String emailVerificationToken;

	@Column(name = "email_verification_expires_at", nullable = true)
	private Instant emailVerificationExpiresAt;

}
