package com.example.packhub.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

	@Bean
	public S3Client s3Client(S3Properties properties) {
		return S3Client.builder()
			.region(Region.of(properties.region()))
			.credentialsProvider(credentials(properties))
			.build();
	}

	@Bean
	public S3Presigner s3Presigner(S3Properties properties) {
		return S3Presigner.builder()
			.region(Region.of(properties.region()))
			.credentialsProvider(credentials(properties))
			.build();
	}

	/** profile이 있으면 그 이름표의 열쇠(로컬), 없으면 AWS 기본 방식(배포 서버의 IAM 역할). 열쇠는 처음 S3를 부를 때 읽는다 */
	private AwsCredentialsProvider credentials(S3Properties properties) {
		if (properties.profile() != null) {
			return ProfileCredentialsProvider.create(properties.profile());
		}
		return DefaultCredentialsProvider.builder().build();
	}
}
