package ca.consmatt.dto;

import java.util.List;

/**
 * Client-provided stocking summary for AI lake fishing tips (Ontario GeoHub–derived data).
 */
public record LakeFishingInsightRequest(
		String waterbody,
		double lat,
		double lng,
		List<String> districts,
		List<String> developmentalStages,
		List<String> speciesStocked,
		List<StockingRow> stockingRows,
		int totalFish) {

	public record StockingRow(String species, int year, int count) {
	}
}
