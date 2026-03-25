package ca.consmatt.dto;

/**
 * Presigned PUT details for direct browser/client uploads to MinIO.
 *
 * @param uploadUrl URL to HTTP PUT the file bytes to
 * @param bucket bucket name
 * @param objectKey key to store (use same key for later GET/presign-get)
 * @param expiresInSeconds lifetime of the signature
 */
public record PresignPutResponse(String uploadUrl, String bucket, String objectKey, int expiresInSeconds) {
}
