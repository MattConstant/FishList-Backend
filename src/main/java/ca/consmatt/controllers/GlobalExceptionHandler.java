package ca.consmatt.controllers;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import ca.consmatt.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Centralized API error mapping so frontend receives stable status codes and error codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));
		return build(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_ERROR",
				"Request validation failed",
				request.getRequestURI(),
				fieldErrors);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiErrorResponse> handleHandlerMethodValidation(
			HandlerMethodValidationException ex,
			HttpServletRequest request) {
		return build(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_ERROR",
				"Request validation failed",
				request.getRequestURI(),
				null);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
			ResponseStatusException ex,
			HttpServletRequest request) {
		HttpStatusCode statusCode = ex.getStatusCode();
		HttpStatus status = HttpStatus.valueOf(statusCode.value());
		return build(
				status,
				mapCode(status),
				ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(),
				request.getRequestURI(),
				null);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
			DataIntegrityViolationException ex,
			HttpServletRequest request) {
		return build(
				HttpStatus.CONFLICT,
				"RESOURCE_CONFLICT",
				"Data conflict",
				request.getRequestURI(),
				null);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
			NoResourceFoundException ex,
			HttpServletRequest request) {
		return build(
				HttpStatus.NOT_FOUND,
				"NOT_FOUND",
				"Resource not found",
				request.getRequestURI(),
				null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleGenericException(
			Exception ex,
			HttpServletRequest request) {
		log.error("Unhandled server error on {}", request.getRequestURI(), ex);
		return build(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"INTERNAL_SERVER_ERROR",
				"An unexpected server error occurred",
				request.getRequestURI(),
				null);
	}

	private ResponseEntity<ApiErrorResponse> build(
			HttpStatus status,
			String code,
			String message,
			String path,
			Map<String, String> errors) {
		return ResponseEntity.status(status).body(new ApiErrorResponse(
				Instant.now(),
				status.value(),
				code,
				message,
				path,
				errors));
	}

	private String mapCode(HttpStatus status) {
		return switch (status) {
		case BAD_REQUEST -> "BAD_REQUEST";
		case UNAUTHORIZED -> "UNAUTHORIZED";
		case FORBIDDEN -> "FORBIDDEN";
		case NOT_FOUND -> "NOT_FOUND";
		case CONFLICT -> "RESOURCE_CONFLICT";
		default -> status.name();
		};
	}
}
