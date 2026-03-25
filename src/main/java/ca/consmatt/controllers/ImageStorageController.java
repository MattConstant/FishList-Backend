package ca.consmatt.controllers;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ca.consmatt.dto.ImageUploadResponse;
import ca.consmatt.dto.PresignGetResponse;
import ca.consmatt.dto.PresignPutRequest;
import ca.consmatt.dto.PresignPutResponse;
import ca.consmatt.storage.ImageStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Image uploads to MinIO: multipart through the API, or presigned PUT for direct client-to-storage
 * transfers.
 */
@RestController
@RequestMapping("/api/storage/images")
@CrossOrigin
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
public class ImageStorageController {

	private final ImageStorageService imageStorageService;

	/**
	 * Upload a single image file; stores under {@code uploads/{username}/...}.
	 *
	 * @param file multipart field name {@code file}
	 * @param authentication current user
	 * @return bucket, key, and a short-lived presigned GET URL
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ImageUploadResponse upload(@RequestParam("file") MultipartFile file, Authentication authentication) {
		return imageStorageService.upload(file, authentication.getName());
	}

	/**
	 * Issues a presigned PUT URL so the client can upload bytes straight to MinIO (efficient for large
	 * files). After PUT, use {@link #presignGet} with the returned {@code objectKey} to obtain a read
	 * URL.
	 */
	@PostMapping(path = "/presign", consumes = MediaType.APPLICATION_JSON_VALUE)
	public PresignPutResponse presignPut(@Valid @RequestBody PresignPutRequest request, Authentication authentication) {
		return imageStorageService.presignPut(request, authentication.getName());
	}

	/**
	 * Presigned GET for an object key you own (same prefix as uploads).
	 *
	 * @param key full object key returned from upload or presign
	 */
	@GetMapping("/download-url")
	public PresignGetResponse presignGet(@RequestParam("key") String key, Authentication authentication) {
		return imageStorageService.presignGet(key, authentication.getName());
	}
}
