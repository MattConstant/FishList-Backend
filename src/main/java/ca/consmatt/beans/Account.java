package ca.consmatt.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

}
