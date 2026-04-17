package ca.consmatt.dto;

import java.util.List;

/**
 * Fishing guidance text. {@code spots} is always empty (legacy field for older clients).
 */
public record LakeFishingInsightResponse(String text, List<FishingSpotPin> spots) {
}
