package ca.consmatt.dto;

/**
 * Presigned GET URL for reading an object without proxying through the API.
 *
 * @param url time-limited read URL
 * @param expiresInSeconds lifetime of the signature
 */
public record PresignGetResponse(String url, int expiresInSeconds) {
}
