package ca.consmatt.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Snapshot of weather, water, and related conditions useful for fishing logs.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
public class Condition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. Sunny, Cloudy, Rain */
    @Size(max = 80, message = "weather must be at most 80 characters")
    private String weather;
    /** Air temperature in Celsius */
    private Double temperature;
    /** Wind speed in km/h */
    @Positive(message = "windSpeed must be positive")
    private Double windSpeed;

    /** e.g. Morning, Afternoon, Evening, Night */
    @Size(max = 50, message = "timeOfDay must be at most 50 characters")
    private String timeOfDay;

    /** Water temperature in Celsius */
    private Double waterTemperature;
    /** e.g. Clear, Murky, Stained */
    @Size(max = 80, message = "waterClarity must be at most 80 characters")
    private String waterClarity;

    /** Barometric pressure */
    @Positive(message = "pressure must be positive")
    private Double pressure;
    /** e.g. Full, New */
    @Size(max = 50, message = "moonPhase must be at most 50 characters")
    private String moonPhase;

    @Column(length = 500)
    @Size(max = 500, message = "notes must be at most 500 characters")
    private String notes;
}