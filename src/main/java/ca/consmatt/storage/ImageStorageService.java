package ca.consmatt.storage;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
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
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
			"image/jpeg",
			"image/png",
			"image/gif",
			"image/webp",
			"image/heic",
			"image/heif");
	private static final Map<String, Set<String>> EXTENSION_TO_CONTENT_TYPES = Map.of(
			".jpg", Set.of("image/jpeg"),
			".jpeg", Set.of("image/jpeg"),
			".png", Set.of("image/png"),
			".gif", Set.of("image/gif"),
			".webp", Set.of("image/webp"),
			".heic", Set.of("image/heic", "image/heif"));
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
		String normalizedContentType = normalizeContentType(request.contentType());
		validateImageContentType(normalizedContentType);
		String ext = extension(request.filename());
		if (ext.isEmpty() || !ALLOWED_EXTENSIONS.contains(ext)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allowed extensions: " + ALLOWED_EXTENSIONS);
		}
		validateExtensionMatchesContentType(ext, normalizedContentType);
		String objectKey = buildObjectKey(username, request.filename());
		try {
			String url = presignClient.getPresignedObjectUrl(
					GetPresignedObjectUrlArgs.builder()
							.method(Method.PUT)
							.bucket(properties.getBucket())
							.object(objectKey)
							.expiry(PRESIGN_PUT_MINUTES, TimeUnit.MINUTES)
							.extraHeaders(java.util.Map.of("Content-Type", normalizedContentType))
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

	public PresignGetResponse presignGet(String objectKey) {
		String normalizedKey = normalizeReadableKey(objectKey);
		try {
			String url = presignedGetUrl(normalizedKey);
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

	private String normalizeReadableKey(String objectKey) {
		if (objectKey == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "object key is required");
		}
		String normalized = objectKey.trim();
		if (normalized.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "object key is required");
		}
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		// Keep reads inside the uploads namespace and block traversal patterns.
		if (!normalized.startsWith("uploads/") || normalized.contains("..")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid object key");
		}
		return normalized;
	}

	private void validateImageMultipart(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
		}
		String normalizedContentType = normalizeContentType(file.getContentType());
		validateImageContentType(normalizedContentType);
		String ext = extension(file.getOriginalFilename());
		if (ext.isEmpty() || !ALLOWED_EXTENSIONS.contains(ext)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allowed extensions: " + ALLOWED_EXTENSIONS);
		}
		validateExtensionMatchesContentType(ext, normalizedContentType);
		validateFileSignature(file, ext);
	}

	private static String normalizeContentType(String contentType) {
		if (contentType == null) {
			return "";
		}
		String normalized = contentType.trim().toLowerCase(Locale.ROOT);
		int semicolon = normalized.indexOf(';');
		return semicolon >= 0 ? normalized.substring(0, semicolon).trim() : normalized;
	}

	private static void validateImageContentType(String contentType) {
		if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Allowed content types: " + ALLOWED_CONTENT_TYPES);
		}
	}

	private static void validateExtensionMatchesContentType(String ext, String contentType) {
		Set<String> allowedTypesForExtension = EXTENSION_TO_CONTENT_TYPES.getOrDefault(ext, Set.of());
		if (!allowedTypesForExtension.contains(contentType)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"File extension does not match Content-Type");
		}
	}

	private static void validateFileSignature(MultipartFile file, String ext) {
		try (InputStream in = file.getInputStream()) {
			byte[] header = in.readNBytes(32);
			String detectedType = detectFileTypeFromHeader(header);
			Set<String> allowedTypes = EXTENSION_TO_CONTENT_TYPES.getOrDefault(ext, Set.of());
			if (detectedType == null || !allowedTypes.contains(detectedType)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File contents are not a valid " + ext + " image");
			}
		} catch (ResponseStatusException e) {
			throw e;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not validate uploaded file type", e);
		}
	}

	private static String detectFileTypeFromHeader(byte[] header) {
		if (header == null || header.length < 12) {
			return null;
		}
		// JPEG
		if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
			return "image/jpeg";
		}
		// PNG
		byte[] pngSig = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
		if (header.length >= 8 && Arrays.equals(Arrays.copyOfRange(header, 0, 8), pngSig)) {
			return "image/png";
		}
		// GIF
		String gifMagic = new String(header, 0, 6, StandardCharsets.US_ASCII);
		if ("GIF87a".equals(gifMagic) || "GIF89a".equals(gifMagic)) {
			return "image/gif";
		}
		// WEBP (RIFF....WEBP)
		String riff = new String(header, 0, 4, StandardCharsets.US_ASCII);
		String webp = new String(header, 8, 4, StandardCharsets.US_ASCII);
		if ("RIFF".equals(riff) && "WEBP".equals(webp)) {
			return "image/webp";
		}
		// HEIC/HEIF (ISO BMFF: ????ftyp....)
		String ftyp = new String(header, 4, 4, StandardCharsets.US_ASCII);
		if ("ftyp".equals(ftyp)) {
			String brand = new String(header, 8, 4, StandardCharsets.US_ASCII).toLowerCase(Locale.ROOT);
			if (Set.of("heic", "heix", "hevc", "hevx", "heif", "mif1", "msf1").contains(brand)) {
				return "image/heic";
			}
		}
		return null;
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
