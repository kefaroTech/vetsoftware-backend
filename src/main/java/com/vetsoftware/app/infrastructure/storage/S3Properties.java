package com.vetsoftware.app.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("vetsoftware.storage.s3")
public record S3Properties(
        String bucket,
        String region,
        String endpoint,
        String accessKey,
        String secretKey,
        boolean pathStyleAccess) {
}
