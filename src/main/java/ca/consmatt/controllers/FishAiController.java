package ca.consmatt.controllers;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ca.consmatt.dto.FishIdentificationResponse;
import ca.consmatt.storage.FishIdentificationRateLimitService;
import ca.consmatt.storage.FishIdentificationService;
import lombok.RequiredArgsConstructor;

/**
 * AI endpoints for fish image identification.
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin
@RequiredArgsConstructor
@ConditionalOnProperty(name = "openai.enabled", havingValue = "true")
public class FishAiController {

	private final FishIdentificationService fishIdentificationService;
	private final FishIdentificationRateLimitService fishIdentificationRateLimitService;

	@PostMapping(path = "/identify-fish", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FishIdentificationResponse identifyFish(@RequestParam("file") MultipartFile file, Authentication authentication) {
		// Authentication is required by security config for /api/** routes.
		fishIdentificationRateLimitService.assertWithinDailyLimit(authentication.getName());
		return fishIdentificationService.identifyFish(file);
	}
}
