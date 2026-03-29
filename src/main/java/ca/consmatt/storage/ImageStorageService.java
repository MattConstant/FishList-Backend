package ca.consmatt.storage;

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import ca.consmatt.dto.ImageUploadResponse;
import ca.consmatt.dto.PresignGetResponse;
import ca.consmatt.dto.PresignPutRequest;
import ca.consmatt.dto.PresignPutResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;

@Service
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
public class ImageStorageService {

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".heic");
	private static final int PRESIGN_PUT_MINUTES = 10;
	private static final int PRESIGN_GET_HOURS = 1;

	private final MinioClient minioClient;
	private final MinioClient presignClient;
	private final MinioProperties properties;

	public ImageStorageService(
			MinioClient minioClient,
			@Qualifier("presignClient") MinioClient presignClient,
			MinioProperties properties) {
		this.minioClient = minioClient;
		this.presignClient = presignClient;
		this.properties = properties;
	}

	public ImageUploadResponse upload(MultipartFile file, String username) {
		validateImageMultipart(file);
		String objectKey = buildObjectKey(username, file.getOriginalFilename());
		String contentType = file.getContentType();
		try (InputStream in = file.getInputStream()) {
			long size = file.getSize();
			var put = PutObjectArgs.builder()
					.bucket(properties.getBucket())
					.object(objectKey)
					.stream(in, size, -1)
					.contentType(contentType)
					.build();
			minioClient.putObject(put);
			String getUrl = "";
			try {
				getUrl = presignedGetUrl(objectKey);
			} catch (Exception ignored) {
				// Upload already succeeded. Some environments can write objects but fail presign URL generation.
				// Return the object key so clients can request /download-url later.
			}
			return new ImageUploadResponse(properties.getBucket(), objectKey, contentType, size, getUrl);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Object storage upload failed", e);
		}
	}

	public PresignPutResponse presignPut(PresignPutRequest request, String username) {
		validateImageContentType(request.contentType());
		String ext = extension(request.filename());
		if (ext.isEmpty() || !ALLOWED_EXTENSIONS.contains(ext)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allowed extensions: " + ALLOWED_EXTENSIONS);
		}
		String objectKey = buildObjectKey(username, request.filename());
		try {
			String url = presignClient.getPresignedObjectUrl(
					GetPresignedObjectUrlArgs.builder()
							.method(Method.PUT)
							.bucket(properties.getBucket())
							.object(objectKey)
							.expiry(PRESIGN_PUT_MINUTES, TimeUnit.MINUTES)
							.extraHeaders(java.util.Map.of("Content-Type", request.contentType()))
							.build());
			return new PresignPutResponse(
					url,
					properties.getBucket(),
					objectKey,
					(int) TimeUnit.MINUTES.toSeconds(PRESIGN_PUT_MINUTES));
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not create presigned PUT URL", e);
		}
	}

	public PresignGetResponse presignGet(String objectKey, String username) {
		assertOwnsKey(objectKey, username);
		try {
			String url = presignedGetUrl(objectKey);
			return new PresignGetResponse(url, (int) TimeUnit.HOURS.toSeconds(PRESIGN_GET_HOURS));
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not create presigned GET URL", e);
		}
	}

	private String presignedGetUrl(String objectKey) throws Exception {
		return presignClient.getPresignedObjectUrl(
				GetPresignedObjectUrlArgs.builder()
						.method(Method.GET)
						.bucket(properties.getBucket())
						.object(objectKey)
						.expiry(PRESIGN_GET_HOURS, TimeUnit.HOURS)
						.build());
	}

	private void assertOwnsKey(String objectKey, String username) {
		String prefix = "uploads/" + username + "/";
		if (objectKey == null || !objectKey.startsWith(prefix)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access this object key");
		}
	}

	private void validateImageMultipart(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
		}
		validateImageContentType(file.getContentType());
		String ext = extension(file.getOriginalFilename());
		if (ext.isEmpty() || !ALLOWED_EXTENSIONS.contains(ext)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allowed extensions: " + ALLOWED_EXTENSIONS);
		}
	}

	private static void validateImageContentType(String contentType) {
		if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content-Type must be an image/* type");
		}
	}

	private String buildObjectKey(String username, String originalFilename) {
		String ext = extension(originalFilename);
		if (ext.isEmpty()) {
			ext = ".bin";
		}
		return "uploads/" + username + "/" + java.util.UUID.randomUUID() + ext;
	}

	private static String extension(String name) {
		if (name == null) {
			return "";
		}
		int i = name.lastIndexOf('.');
		if (i < 0) {
			return "";
		}
		return name.substring(i).toLowerCase(Locale.ROOT);
	}
}
