package ca.consmatt.storage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.consmatt.dto.FishIdentificationResponse;

/**
 * Calls OpenAI vision-capable chat models to suggest fish species from an uploaded image.
 */
@Service
@ConditionalOnProperty(name = "openai.enabled", havingValue = "true")
public class FishIdentificationService {
	private static final Logger log = LoggerFactory.getLogger(FishIdentificationService.class);

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
			"image/jpeg",
			"image/png",
			"image/gif",
			"image/webp",
			"image/heic",
			"image/heif");

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${openai.api-key:}")
	private String apiKey;

	@Value("${openai.model:gpt-4.1-mini}")
	private String model;

	public FishIdentificationResponse identifyFish(MultipartFile file) {
		validateImage(file);
		if (apiKey == null || apiKey.isBlank()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OpenAI API key is not configured");
		}
		String contentType = normalizeContentType(file.getContentType());
		try {
			String dataUrl = "data:" + contentType + ";base64,"
					+ Base64.getEncoder().encodeToString(file.getBytes());

			Map<String, Object> payload = Map.of(
					"model", model,
					"temperature", 0.2,
					"response_format", Map.of("type", "json_object"),
					"messages", List.of(
							Map.of(
									"role", "system",
									"content",
									"You are an expert fish species identifier. Return strict JSON with keys: suggestedSpecies (string), confidence (number 0..1), message (string)."),
							Map.of(
									"role", "user",
									"content", List.of(
											Map.of("type", "text",
													"text",
													"Identify the fish species in this image. If uncertain, provide your best guess and explain uncertainty in message."),
											Map.of("type", "image_url",
													"image_url", Map.of("url", dataUrl))))));

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
				log.warn("OpenAI fish identification failed: status={}, body={}", response.statusCode(), upstreamBody);
				String upstreamMessage = extractUpstreamMessage(response.body());
				throw new ResponseStatusException(
						HttpStatus.BAD_GATEWAY,
						"AI fish identification request failed: "
								+ (upstreamMessage.isBlank() ? ("status " + response.statusCode()) : upstreamMessage));
			}

			JsonNode root = objectMapper.readTree(response.body());
			String content = root.path("choices").path(0).path("message").path("content").asText("");
			if (content.isBlank()) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI response was empty");
			}

			JsonNode parsed = parseModelJson(content);
			String suggestedSpecies = parsed.path("suggestedSpecies").asText("").trim();
			if (suggestedSpecies.isBlank()) {
				suggestedSpecies = "Unknown";
			}
			Double confidence = parsed.has("confidence") && parsed.get("confidence").isNumber()
					? parsed.get("confidence").asDouble()
					: null;
			String message = parsed.path("message").asText("").trim();
			return new FishIdentificationResponse(suggestedSpecies, confidence, message);
		} catch (ResponseStatusException e) {
			throw e;
		} catch (Exception e) {
			log.error("Fish identification failed unexpectedly", e);
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not identify fish from image", e);
		}
	}

	private JsonNode parseModelJson(String content) throws Exception {
		String trimmed = content.trim();
		try {
			return objectMapper.readTree(trimmed);
		} catch (Exception ignored) {
			// Some models still return fenced JSON despite response_format hints.
		}
		String normalized = trimmed
				.replaceFirst("^```json\\s*", "")
				.replaceFirst("^```\\s*", "")
				.replaceFirst("\\s*```$", "")
				.trim();
		return objectMapper.readTree(normalized);
	}

	private void validateImage(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
		}
		String contentType = normalizeContentType(file.getContentType());
		if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image type");
		}
	}

	private static String normalizeContentType(String contentType) {
		if (contentType == null) {
			return "";
		}
		String normalized = contentType.trim().toLowerCase(Locale.ROOT);
		int semicolon = normalized.indexOf(';');
		return semicolon >= 0 ? normalized.substring(0, semicolon).trim() : normalized;
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
		if (value == null) return "";
		if (value.length() <= maxLen) return value;
		return value.substring(0, maxLen) + "...";
	}
}
