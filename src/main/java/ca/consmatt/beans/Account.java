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
	 * Legacy field; unused for Google-only auth (nullable in DB).
	 */
	@JsonIgnore
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AccountRole role = AccountRole.USER;

}
