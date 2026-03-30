package ca.consmatt.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reference data describing a fish species (averages, description); not a per-trip catch record.
 */
@Entity
@Table(name = "fish")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "species is required")
    @Size(max = 120, message = "species must be at most 120 characters")
    private String species;

    @Positive(message = "averageWeight must be positive")
    private Double averageWeight;

    @Positive(message = "averageLength must be positive")
    private Double averageLength;

    @Positive(message = "maxWeight must be positive")
    private Double maxWeight;

    @Positive(message = "maxLength must be positive")
    private Double maxLength;

    @Size(max = 2048, message = "imageUrl must be at most 2048 characters")
    private String imageUrl;

    @Column(length = 1000)
    @Size(max = 1000, message = "description must be at most 1000 characters")
    private String description;
}
