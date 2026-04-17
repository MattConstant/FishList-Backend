package ca.consmatt.storage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.consmatt.dto.FishingSpotPin;
import ca.consmatt.dto.LakeFishingInsightRequest;
import ca.consmatt.dto.LakeFishingInsightResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calls OpenAI for Ontario lake fishing tips (species, tactics, general guidance). No map coordinates.
 */
@Service
@ConditionalOnProperty(name = "openai.enabled", havingValue = "true")
public class LakeFishingInsightService {
	private static final Logger log = LoggerFactory.getLogger(LakeFishingInsightService.class);

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${openai.api-key:}")
	private String apiKey;

	@Value("${openai.model:gpt-4.1-mini}")
	private String model;

	public LakeFishingInsightResponse insights(LakeFishingInsightRequest req) {
		validate(req);
		if (apiKey == null || apiKey.isBlank()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OpenAI API key is not configured");
		}

		String stockingLines = buildStockingLines(req.stockingRows());

		String system = """
				You are a practical fishing guide for Ontario, Canada. The app shows MNRF stocking data for this waterbody — your job is TEXT ONLY: help anglers understand what they might catch and how to fish here.

				Respond with STRICT JSON only (no markdown fences). Schema:
				{
				  "narrative": string (plain text, under 900 words, sections/bullets ok)
				}

				In the narrative, emphasize:
				- The stocked species listed and what anglers typically target here (seasonal notes ok).
				- Simple, practical tips: tackle ideas, depths/structure to try, bait/lure styles — stay general (no exact GPS or “fish here on the map”).
				- Access, regulations, limits, and safety (ice/boat) as brief reminders.

				Do NOT output coordinates, bearings, distances, or map pins. Do NOT invent stocking facts beyond what the user data suggests.""";

		String user = "Waterbody: " + req.waterbody() + "\n"
				+ "Approximate stocking reference (lat, lng) — for context only, do not give map directions: "
				+ String.format(Locale.US, "%.5f, %.5f", req.lat(), req.lng()) + "\n"
				+ "MNRF district(s): " + String.join(", ", nullSafe(req.districts())) + "\n"
				+ "Species in stocking data: " + String.join(", ", nullSafe(req.speciesStocked())) + "\n"
				+ "Total fish (summary): " + req.totalFish() + "\n"
				+ "Developmental stages: " + String.join(", ", nullSafe(req.developmentalStages())) + "\n\n"
				+ "Recent stocking rows:\n"
				+ (stockingLines.isBlank() ? "(none)" : stockingLines)
				+ "\n\nReturn JSON with only the narrative field as specified.";

		try {
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("model", model);
			payload.put("temperature", 0.45);
			payload.put("max_tokens", 1800);
			payload.put("response_format", Map.of("type", "json_object"));
			payload.put(
					"messages",
					List.of(
							Map.of("role", "system", "content", system),
							Map.of("role", "user", "content", user)));

			String body = objectMapper.writeValueAsString(payload);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://api.openai.com/v1/chat/completions"))
					.header("Authorization", "Bearer " + apiKey)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(body))
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() >= 400) {
				String upstreamBody = truncate(response.body(), 1000);
				log.warn("OpenAI lake insights failed: status={}, body={}", response.statusCode(), upstreamBody);
				String upstreamMessage = extractUpstreamMessage(response.body());
				throw new ResponseStatusException(
						HttpStatus.BAD_GATEWAY,
						"AI lake insight request failed: "
								+ (upstreamMessage.isBlank() ? ("status " + response.statusCode()) : upstreamMessage));
			}

			JsonNode root = objectMapper.readTree(response.body());
			String content = root.path("choices").path(0).path("message").path("content").asText("");
			if (content.isBlank()) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI response was empty");
			}

			JsonNode parsed = parseModelJson(content);
			String narrative = parsed.path("narrative").asText("").trim();
			if (narrative.isBlank()) {
				narrative = parsed.path("text").asText("").trim();
			}
			if (narrative.isBlank()) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI JSON missing narrative");
			}

			return new LakeFishingInsightResponse(narrative, List.<FishingSpotPin>of());
		} catch (ResponseStatusException e) {
			throw e;
		} catch (Exception e) {
			log.error("Lake fishing insight failed unexpectedly", e);
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not generate lake fishing insights", e);
		}
	}

	private JsonNode parseModelJson(String content) throws Exception {
		String trimmed = content.trim();
		try {
			return objectMapper.readTree(trimmed);
		} catch (Exception ignored) {
			// fall through
		}
		String normalized = trimmed
				.replaceFirst("^```(?:json)?\\s*", "")
				.replaceFirst("\\s*```\\s*$", "")
				.trim();
		return objectMapper.readTree(normalized);
	}

	private void validate(LakeFishingInsightRequest req) {
		if (req == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required");
		}
		if (req.waterbody() == null || req.waterbody().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "waterbody is required");
		}
		if (!Double.isFinite(req.lat()) || !Double.isFinite(req.lng())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lat and lng must be finite");
		}
		if (req.stockingRows() != null && req.stockingRows().size() > 80) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "too many stocking rows");
		}
	}

	private static List<String> nullSafe(List<String> list) {
		return list == null ? List.of() : list;
	}

	private static String buildStockingLines(List<LakeFishingInsightRequest.StockingRow> rows) {
		if (rows == null || rows.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		int n = 0;
		for (LakeFishingInsightRequest.StockingRow r : rows) {
			if (n >= 35) {
				break;
			}
			if (r == null || r.species() == null) {
				continue;
			}
			sb.append(r.species())
					.append(" (")
					.append(r.year())
					.append("): ")
					.append(r.count())
					.append(" fish\n");
			n++;
		}
		return sb.toString();
	}

	private String extractUpstreamMessage(String body) {
		try {
			JsonNode root = objectMapper.readTree(body);
			JsonNode messageNode = root.path("error").path("message");
			if (messageNode.isTextual()) {
				return messageNode.asText();
			}
		} catch (Exception ignored) {
			// fall through
		}
		return "";
	}

	private static String truncate(String value, int maxLen) {
		if (value == null) {
			return "";
		}
		if (value.length() <= maxLen) {
			return value;
		}
		return value.substring(0, maxLen) + "...";
	}
}
