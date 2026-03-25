package ca.consmatt.storage;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
public class MinioBucketInitializer implements ApplicationRunner {

	private final MinioClient minioClient;
	private final MinioProperties properties;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String bucket = properties.getBucket();
		boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
		if (!exists) {
			minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
		}
	}
}
