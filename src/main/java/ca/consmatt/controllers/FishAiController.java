package ca.consmatt.controllers;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ca.consmatt.dto.FishIdentificationResponse;
import ca.consmatt.dto.LakeFishingInsightRequest;
import ca.consmatt.dto.LakeFishingInsightResponse;
import ca.consmatt.storage.FishIdentificationRateLimitService;
import ca.consmatt.storage.FishIdentificationService;
import ca.consmatt.storage.LakeFishingInsightRateLimitService;
import ca.consmatt.storage.LakeFishingInsightService;
import lombok.RequiredArgsConstructor;

/**
 * AI endpoints: fish image identification and lake fishing insights.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "openai.enabled", havingValue = "true")
public class FishAiController {

	private final FishIdentificationService fishIdentificationService;
	private final FishIdentificationRateLimitService fishIdentificationRateLimitService;
	private final LakeFishingInsightService lakeFishingInsightService;
	private final LakeFishingInsightRateLimitService lakeFishingInsightRateLimitService;

	@PostMapping(path = "/identify-fish", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FishIdentificationResponse identifyFish(@RequestParam("file") MultipartFile file, Authentication authentication) {
		// Authentication is required by security config for /api/** routes.
		fishIdentificationRateLimitService.assertWithinDailyLimit(authentication.getName());
		return fishIdentificationService.identifyFish(file);
	}

	@PostMapping(path = "/lake-fishing-insights", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public LakeFishingInsightResponse lakeFishingInsights(
			@RequestBody LakeFishingInsightRequest body,
			Authentication authentication) {
		lakeFishingInsightRateLimitService.assertWithinDailyLimit(authentication.getName());
		return lakeFishingInsightService.insights(body);
	}
}
