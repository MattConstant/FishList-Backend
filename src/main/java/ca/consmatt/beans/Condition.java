package ca.consmatt.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    private String weather;
    /** Air temperature in Celsius */
    private Double temperature;
    /** Wind speed in km/h */
    private Double windSpeed;

    /** e.g. Morning, Afternoon, Evening, Night */
    private String timeOfDay;

    /** Water temperature in Celsius */
    private Double waterTemperature;
    /** e.g. Clear, Murky, Stained */
    private String waterClarity;

    /** Barometric pressure */
    private Double pressure;
    /** e.g. Full, New */
    private String moonPhase;

    @Column(length = 500)
    private String notes;
}