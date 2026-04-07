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
	/** AWS SigV4 region (required for Supabase Storage S3; copy from Storage settings in dashboard). */
	private String region = "us-east-1";
	/**
	 * Path-style addressing (bucket in URL path). Required for Supabase Storage S3
	 * ({@code https://&lt;ref&gt;.storage.supabase.co/storage/v1/s3}).
	 */
	private boolean pathStyle = false;
}
