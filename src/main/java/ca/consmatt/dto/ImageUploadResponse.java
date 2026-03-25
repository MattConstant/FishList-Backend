package ca.consmatt.dto;

/**
 * Result of a server-side multipart upload to MinIO.
 *
 * @param bucket bucket name
 * @param objectKey object path within the bucket
 * @param contentType stored content type
 * @param sizeBytes byte length
 * @param getUrl time-limited presigned GET URL for browsers (host adjusted for Docker if configured)
 */
public record ImageUploadResponse(
		String bucket,
		String objectKey,
		String contentType,
		long sizeBytes,
		String getUrl) {
}
