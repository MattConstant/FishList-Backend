package ca.consmatt.dto;

import ca.consmatt.beans.Condition;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating or updating a {@link ca.consmatt.beans.Condition} snapshot (API contract; not a JPA entity).
 */
public record CreateConditionRequest(
		@Size(max = 80, message = "weather must be at most 80 characters")
		String weather,
		Double temperature,
		@Positive(message = "windSpeed must be positive")
		Double windSpeed,
		@Size(max = 50, message = "timeOfDay must be at most 50 characters")
		String timeOfDay,
		Double waterTemperature,
		@Size(max = 80, message = "waterClarity must be at most 80 characters")
		String waterClarity,
		@Positive(message = "pressure must be positive")
		Double pressure,
		@Size(max = 50, message = "moonPhase must be at most 50 characters")
		String moonPhase,
		@Size(max = 500, message = "notes must be at most 500 characters")
		String notes) {

	/** Maps this request to a new persistent {@link ca.consmatt.beans.Condition} (no id; assigned on save). */
	public Condition toNewEntity() {
		return Condition.builder()
				.weather(weather)
				.temperature(temperature)
				.windSpeed(windSpeed)
				.timeOfDay(timeOfDay)
				.waterTemperature(waterTemperature)
				.waterClarity(waterClarity)
				.pressure(pressure)
				.moonPhase(moonPhase)
				.notes(notes)
				.build();
	}

	/** Copies fields onto an existing managed {@link ca.consmatt.beans.Condition} (e.g. update by id). */
	public void applyTo(Condition target) {
		target.setWeather(weather);
		target.setTemperature(temperature);
		target.setWindSpeed(windSpeed);
		target.setTimeOfDay(timeOfDay);
		target.setWaterTemperature(waterTemperature);
		target.setWaterClarity(waterClarity);
		target.setPressure(pressure);
		target.setMoonPhase(moonPhase);
		target.setNotes(notes);
	}
}
