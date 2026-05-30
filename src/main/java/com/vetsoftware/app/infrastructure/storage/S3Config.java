package com.vetsoftware.app.infrastructure.storage;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(S3Properties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider(properties));

        // endpoint personalizado (LocalStack / MinIO); en AWS real se deja vacío
        if (StringUtils.hasText(properties.endpoint())) {
            builder.endpointOverride(URI.create(properties.endpoint()))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(properties.pathStyleAccess())
                            .build());
        }
        return builder.build();
    }

    private AwsCredentialsProvider credentialsProvider(S3Properties properties) {
        // si hay credenciales explícitas se usan; si no, cadena por defecto (env vars, IAM role…)
        if (StringUtils.hasText(properties.accessKey()) && StringUtils.hasText(properties.secretKey())) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
        }
        return DefaultCredentialsProvider.create();
    }
}
