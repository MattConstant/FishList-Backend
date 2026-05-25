package ca.consmatt.dto;

/**
 * Aggregate counts for the marketing landing page ({@code GET /api/public/stats}).
 */
public record PublicStatsResponse(
		long catchesLogged,
		long lakesMapped,
		long speciesTracked,
		long tripsPlanned) {
}
