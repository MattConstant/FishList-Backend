package ca.consmatt.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request a presigned PUT URL so the client can upload bytes directly to MinIO.
 *
 * @param filename original file name (used for extension only)
 * @param contentType MIME type for the upcoming PUT (e.g. image/jpeg)
 */
public record PresignPutRequest(@NotBlank String filename, @NotBlank String contentType) {
}
