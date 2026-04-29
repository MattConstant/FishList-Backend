package ca.consmatt.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A fishing spot posted by a user. {@link #account} is hidden from JSON; use {@link #getAccountId()}.
 */
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Data
@Entity
public class Location {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@NonNull
	@NotBlank(message = "locationName is required")
	@Size(max = 120, message = "locationName must be at most 120 characters")
	private String locationName;
	
	@NonNull
	@NotBlank(message = "latitude is required")
	@Size(max = 40, message = "latitude must be at most 40 characters")
	private String latitude;
	
	@NonNull
	@NotBlank(message = "longitude is required")
	@Size(max = 40, message = "longitude must be at most 40 characters")
	private String longitude;
	
	@NonNull
	@NotBlank(message = "timeStamp is required")
	@Size(max = 80, message = "timeStamp must be at most 80 characters")
	private String timeStamp;
	
	@Size(max = 1000, message = "details must be at most 1000 characters")
	private String Details;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id")
	@JsonIgnore
	private Account account;

	/**
	 * Null in older rows is treated as {@link PostVisibility#PUBLIC} in queries.
	 * {@code columnDefinition} keeps Hibernate from auto-adding a {@code CHECK} that would block
	 * future enum additions.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "visibility", length = 16, columnDefinition = "varchar(16)")
	private PostVisibility visibility;

	/**
	 * Body of water (lake, river, ocean, etc.); {@code null} when not specified.
	 * Uses {@code columnDefinition} for the same reason as {@code visibility}.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "water_type", length = 32, columnDefinition = "varchar(32)")
	private WaterType waterType;

	/**
	 * @return owning account id for API responses, or {@code null} if not loaded
	 */
	public Long getAccountId() {
		return account != null ? account.getId() : null;
	}

}
	