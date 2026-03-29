package ca.consmatt.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
public class MinioClientConfig {

	/** Client for server-side operations (putObject, bucketExists, etc.). Uses the internal endpoint. */
	@Bean
	public MinioClient minioClient(MinioProperties properties) {
		return MinioClient.builder()
				.endpoint(properties.getEndpoint())
				.credentials(properties.getAccessKey(), properties.getSecretKey())
				.build();
	}

	/**
	 * Client used only for generating presigned URLs the browser will access directly.
	 * Points at the public base URL so the S3 signature matches the Host header the browser sends.
	 */
	@Bean
	public MinioClient presignClient(MinioProperties properties) {
		String endpoint = properties.getPublicBaseUrl();
		if (endpoint == null || endpoint.isBlank()) {
			endpoint = properties.getEndpoint();
		}
		return MinioClient.builder()
				.endpoint(endpoint)
				// Avoid runtime region lookup against host-mapped endpoints (e.g. localhost in Docker).
				.region("us-east-1")
				.credentials(properties.getAccessKey(), properties.getSecretKey())
				.build();
	}
}
