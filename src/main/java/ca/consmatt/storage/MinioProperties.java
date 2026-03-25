package ca.consmatt.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Settings for the MinIO (S3-compatible) object store.
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

	private boolean enabled = false;
	private String endpoint = "http://localhost:9000";
	private String accessKey = "minioadmin";
	private String secretKey = "minioadmin";
	private String bucket = "fishlist";
	/** Used to rewrite presigned URLs when the SDK uses an internal Docker hostname (e.g. minio:9000). */
	private String publicBaseUrl = "http://localhost:9000";
}
