package ca.consmatt.storage;

import java.net.URI;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3-compatible object storage (MinIO, Supabase Storage, AWS S3). Uses AWS SDK v2 so endpoints may
 * include a path prefix (e.g. Supabase {@code .../storage/v1/s3}), which the MinIO Java client rejects.
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
public class S3ObjectStorageConfig {

	@Bean
	public S3Client s3Client(MinioProperties properties) {
		return buildS3Client(parseUri(properties.getEndpoint()), properties);
	}

	@Bean
	public S3Presigner s3Presigner(MinioProperties properties) {
		String pub = properties.getPublicBaseUrl();
		if (pub == null || pub.isBlank()) {
			pub = properties.getEndpoint();
		}
		return buildPresigner(parseUri(pub), properties);
	}

	private static URI parseUri(String endpoint) {
		return URI.create(endpoint.trim());
	}

	private static StaticCredentialsProvider credentialsProvider(MinioProperties p) {
		return StaticCredentialsProvider.create(AwsBasicCredentials.create(p.getAccessKey(), p.getSecretKey()));
	}

	private static S3Configuration s3Configuration(MinioProperties p) {
		return S3Configuration.builder()
				.pathStyleAccessEnabled(p.isPathStyle())
				.build();
	}

	private static S3Client buildS3Client(URI endpoint, MinioProperties p) {
		// Large phone photos (10–20 MB+) need generous SDK timeouts so PutObject can finish cleanly.
		ClientOverrideConfiguration timeouts = ClientOverrideConfiguration.builder()
				.apiCallTimeout(Duration.ofMinutes(5))
				.apiCallAttemptTimeout(Duration.ofMinutes(5))
				.build();
		return S3Client.builder()
				.endpointOverride(endpoint)
				.region(Region.of(p.getRegion()))
				.credentialsProvider(credentialsProvider(p))
				.serviceConfiguration(s3Configuration(p))
				.overrideConfiguration(timeouts)
				.build();
	}

	private static S3Presigner buildPresigner(URI endpoint, MinioProperties p) {
		return S3Presigner.builder()
				.endpointOverride(endpoint)
				.region(Region.of(p.getRegion()))
				.credentialsProvider(credentialsProvider(p))
				.serviceConfiguration(s3Configuration(p))
				.build();
	}
}
