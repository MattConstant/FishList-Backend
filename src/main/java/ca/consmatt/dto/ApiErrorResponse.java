package ca.consmatt.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Standard API error payload for frontend-friendly handling.
 *
 * @param timestamp UTC timestamp when the error was generated
 * @param status HTTP status code
 * @param code stable machine-readable application error code
 * @param message human-readable message
 * @param path request path
 * @param errors optional field-level validation errors
 */
public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String code,
		String message,
		String path,
		Map<String, String> errors) {
}
