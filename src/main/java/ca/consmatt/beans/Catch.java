package ca.consmatt.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

import ca.consmatt.dto.FishDetailResponse;

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

	@Column(name = "image_urls", length = 8192)
	private String imageUrlsRaw;

	@Column(length = 1000)
	private String description;

	/** JSON array of per-fish lines when one post lists multiple fish; {@code null} for legacy rows. */
	@Column(name = "fish_details_json", length = 8192)
	@JsonIgnore
	private String fishDetailsJson;

	private static final ObjectMapper FISH_JSON = new ObjectMapper();

	@JsonProperty("fishDetails")
	public List<FishDetailResponse> getFishDetails() {
		if (fishDetailsJson == null || fishDetailsJson.isBlank()) {
			return null;
		}
		try {
			return FISH_JSON.readValue(fishDetailsJson, new TypeReference<List<FishDetailResponse>>() {
			});
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @return parent location id for JSON (location entity is not serialized)
	 */
	@JsonProperty("locationId")
	public Long getLocationId() {
		return location != null ? location.getId() : null;
	}

	/**
	 * Multi-image API view; persisted as newline-delimited keys/URLs.
	 */
	@JsonProperty("imageUrls")
	public List<String> getImageUrlsList() {
		if (imageUrlsRaw != null && !imageUrlsRaw.isBlank()) {
			return Arrays.stream(imageUrlsRaw.split("\\R"))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.limit(4)
					.toList();
		}
		if (imageUrl != null && !imageUrl.isBlank()) {
			return List.of(imageUrl);
		}
		return List.of();
	}

	@JsonProperty("imageUrls")
	public void setImageUrlsList(List<String> imageUrlsList) {
		if (imageUrlsList == null || imageUrlsList.isEmpty()) {
			this.imageUrlsRaw = null;
			return;
		}
		this.imageUrlsRaw = imageUrlsList.stream()
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.limit(4)
				.collect(java.util.stream.Collectors.joining("\n"));
	}
}
