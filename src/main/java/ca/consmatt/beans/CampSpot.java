package ca.consmatt.beans;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A pinned camping spot on the map. This is intentionally separate from {@link Location}/{@link Catch}
 * so it doesn't appear in the fishing feed.
 */
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Data
@Entity
@Table(name = "camp_spots")
public class CampSpot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NonNull
	@NotBlank(message = "name is required")
	@Size(max = 120, message = "name must be at most 120 characters")
	@Column(nullable = false, length = 120)
	private String name;

	@NonNull
	@NotBlank(message = "latitude is required")
	@Size(max = 40, message = "latitude must be at most 40 characters")
	@Column(nullable = false, length = 40)
	private String latitude;

	@NonNull
	@NotBlank(message = "longitude is required")
	@Size(max = 40, message = "longitude must be at most 40 characters")
	@Column(nullable = false, length = 40)
	private String longitude;

	@NonNull
	@NotBlank(message = "timeStamp is required")
	@Size(max = 80, message = "timeStamp must be at most 80 characters")
	@Column(nullable = false, length = 80)
	private String timeStamp;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id")
	@JsonIgnore
	private Account account;

	/**
	 * Null in older rows is treated as {@link PostVisibility#PUBLIC}.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "visibility", length = 16, columnDefinition = "varchar(16)")
	private PostVisibility visibility;

	/**
	 * Optional photo object keys or absolute URLs (same conventions as catch images).
	 * Stored in a separate table via {@link ElementCollection}.
	 */
	@ElementCollection
	@CollectionTable(name = "camp_spot_images", joinColumns = @JoinColumn(name = "camp_spot_id"))
	@Column(name = "image_url", length = 600)
	private List<String> imageUrls = new ArrayList<>();

	public Long getAccountId() {
		return account != null ? account.getId() : null;
	}
}

