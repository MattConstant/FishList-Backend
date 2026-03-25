package ca.consmatt.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single catch event at a location; species and measurements are stored inline (no FK to catalog).
 */
@Entity
@Table(name = "catches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Catch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id")
	@JsonIgnore
	private Location location;

	/** Species or common name, e.g. "Brook trout" */
	@Column(nullable = false)
	private String species;

	private Integer quantity;

	private Double lengthCm;

	private Double weightKg;

	@Column(length = 500)
	private String notes;

	@Column(length = 2048)
	private String imageUrl;

	@Column(length = 1000)
	private String description;

	/**
	 * @return parent location id for JSON (location entity is not serialized)
	 */
	@JsonProperty("locationId")
	public Long getLocationId() {
		return location != null ? location.getId() : null;
	}
}
