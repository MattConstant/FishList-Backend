package ca.consmatt.dto;

import java.util.List;

/**
 * Paginated admin account listing for {@code GET /api/admin/accounts}.
 */
public record AdminAccountPageResponse(
		List<AdminAccountRowResponse> content,
		long totalElements,
		int totalPages,
		int page,
		int size) {
}
