package ca.consmatt.controllers;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;

import ca.consmatt.dto.ImageUploadResponse;
import ca.consmatt.dto.PresignGetResponse;
import ca.consmatt.dto.PresignPutRequest;
import ca.consmatt.dto.PresignPutResponse;
import ca.consmatt.storage.ImageUploadRateLimitService;
import ca.consmatt.storage.ImageStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

/**
 * Image uploads to S3-compatible storage (MinIO, Supabase Storage, etc.): multipart or presigned PUT.
 * transfers.
 */
@RestController
@RequestMapping("/api/storage/images")
@CrossOrigin
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
@Validated
public class ImageStorageController {

	private static final Logger log = LoggerFactory.getLogger(ImageStorageController.class);

	private final ImageStorageService imageStorageService;
	private final ImageUploadRateLimitService imageUploadRateLimitService;

	/**
	 * Upload a single image file; stores under {@code uploads/{username}/...}.
	 *
	 * @param file multipart field name {@code file}
	 * @param authentication current user
	 * @return bucket, key, and a short-lived presigned GET URL
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ImageUploadResponse upload(
			@RequestParam("file") MultipartFile file,
			Authentication authentication,
			HttpServletRequest request) {
		String username = authentication.getName();
		String clientIp = resolveClientIp(request);
		log.info(
				"IMAGE_UPLOAD_ATTEMPT user={} ip={} filename={} sizeBytes={} contentType={}",
				username,
				clientIp,
				file.getOriginalFilename(),
				file.getSize(),
				file.getContentType());
		imageUploadRateLimitService.assertWithinDailyLimit(username);
		ImageUploadResponse response = imageStorageService.upload(file, username);
		log.info("IMAGE_UPLOAD_SUCCESS user={} ip={} objectKey={}", username, clientIp, response.objectKey());
		return response;
	}

	/**
	 * Issues a presigned PUT URL so the client can upload bytes straight to MinIO (efficient for large
	 * files). After PUT, use {@link #presignGet} with the returned {@code objectKey} to obtain a read
	 * URL.
	 */
	@PostMapping(path = "/presign", consumes = MediaType.APPLICATION_JSON_VALUE)
	public PresignPutResponse presignPut(
			@Valid @RequestBody PresignPutRequest requestBody,
			Authentication authentication,
			HttpServletRequest request) {
		String username = authentication.getName();
		String clientIp = resolveClientIp(request);
		log.info(
				"IMAGE_PRESIGN_ATTEMPT user={} ip={} filename={} contentType={}",
				username,
				clientIp,
				requestBody.filename(),
				requestBody.contentType());
		imageUploadRateLimitService.assertWithinDailyLimit(username);
		PresignPutResponse response = imageStorageService.presignPut(requestBody, username);
		log.info("IMAGE_PRESIGN_SUCCESS user={} ip={} objectKey={}", username, clientIp, response.objectKey());
		return response;
	}

	/**
	 * Presigned GET for a catch image object key in the uploads namespace.
	 *
	 * @param key full object key returned from upload or presign
	 */
	@GetMapping("/download-url")
	public PresignGetResponse presignGet(@RequestParam("key") @NotBlank(message = "key is required") String key) {
		return imageStorageService.presignGet(key);
	}

	private String resolveClientIp(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isBlank()) {
			String firstIp = xForwardedFor.split(",")[0].trim();
			if (!firstIp.isBlank()) {
				return firstIp;
			}
		}
		String xRealIp = request.getHeader("X-Real-IP");
		if (xRealIp != null && !xRealIp.isBlank()) {
			return xRealIp.trim();
		}
		String remoteAddr = request.getRemoteAddr();
		return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
	}
}
