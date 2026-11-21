package com.example.packhub.global.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param profile         로컬에서 쓸 ~/.aws/credentials 이름표. 없으면 AWS 기본 방식(EC2 IAM 역할 등)을 쓴다
 * @param uploadUrlExpiry 업로드 URL(presigned PUT) 유효 시간
 * @param viewUrlExpiry   조회 URL(presigned GET) 유효 시간
 */
@ConfigurationProperties("packhub.s3")
public record S3Properties(
	String bucket,
	String region,
	String profile,
	Duration uploadUrlExpiry,
	Duration viewUrlExpiry
) {
}
