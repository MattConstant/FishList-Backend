package ca.consmatt.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
public class MinioBucketInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MinioBucketInitializer.class);

	private final S3Client s3Client;
	private final MinioProperties properties;

	@Override
	public void run(ApplicationArguments args) {
		String bucket = properties.getBucket();
		try {
			s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
		} catch (S3Exception e) {
			int code = e.statusCode();
			if (code != 404 && code != 400) {
				log.warn("Could not verify bucket '{}': {}", bucket, e.getMessage());
				return;
			}
			try {
				s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
				log.info("Created object storage bucket '{}'", bucket);
			} catch (Exception ex) {
				log.warn(
						"Could not create bucket '{}'. If you use Supabase Storage, create the bucket in the dashboard. Cause: {}",
						bucket,
						ex.getMessage());
			}
		} catch (Exception e) {
			log.warn("Bucket check failed for '{}': {}", bucket, e.getMessage());
		}
	}
}
