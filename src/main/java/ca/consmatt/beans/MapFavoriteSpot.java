package ca.consmatt.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A map pin bookmarked by a user (forecast / stocking / presence tap). Matches the web client’s
 * five-decimal “spot key” for idempotent adds.
 */
@Entity
@Table(name = "map_favorite_spots", uniqueConstraints = {
		@UniqueConstraint(name = "uk_map_favorite_account_spot_key", columnNames = { "account_id", "spot_key" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapFavoriteSpot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id")
	@JsonIgnore
	private Account account;

	/**
	 * Same format as {@code makeFavoriteSpotId} on the map page: snapped lat,lng separated by '|'.
	 */
	@Column(name = "spot_key", nullable = false, length = 48)
	private String spotKey;

	@Column(nullable = false)
	private double latitude;

	@Column(nullable = false)
	private double longitude;

	@Column(nullable = false, length = 200)
	private String label;

	/** ISO-8601 instant string (consistent with friendship rows). */
	@Column(name = "created_at", nullable = false, length = 48)
	private String createdAt;
}
