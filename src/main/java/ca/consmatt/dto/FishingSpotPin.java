package ca.consmatt.dto;

/**
 * Map pin for an AI-suggested fishing area (lat/lng computed from the stocking point).
 */
public record FishingSpotPin(double lat, double lng, String label) {
}
