package ca.consmatt.dto;

public record AdminSummaryResponse(
		long totalAccounts,
		long totalLocations,
		long totalCatches,
		long totalComments,
		long totalLikes,
		long totalFriendships) {
}
