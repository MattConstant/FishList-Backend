package ca.consmatt.dto;

public record AdminAccountRowResponse(
		Long id,
		String username,
		long locations,
		long catches,
		long comments,
		long likes) {
}
