package ca.consmatt.dto;

/**
 * JSON view for one fish line read from {@link ca.consmatt.beans.Catch#fishDetailsJson}.
 */
public record FishDetailResponse(String species, Double lengthCm, Double weightKg, String notes) {
}
