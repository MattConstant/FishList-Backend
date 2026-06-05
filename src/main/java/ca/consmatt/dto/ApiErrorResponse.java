package ca.consmatt.dto;

import java.time.Instant;
import java.util.Map;

/**
 * JSON error body for API failures (see {@link ca.consmatt.controllers.GlobalExceptionHandler}).
 */
public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String code,
		String message,
		String path,
		Map<String, String> errors) {
}
