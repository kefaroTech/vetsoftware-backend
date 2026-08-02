package com.vetsoftware.app.infrastructure.audit.outbox;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.firehose.FirehoseClient;
import software.amazon.awssdk.services.firehose.FirehoseClientBuilder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuditOutboxProperties.class)
public class AuditOutboxConfig {

  @Bean
  @ConditionalOnProperty(
      prefix = "vetsoftware.audit.outbox",
      name = "publisher-enabled",
      havingValue = "true")
  FirehoseClient auditFirehoseClient(AuditOutboxProperties properties) {
    properties.validate();
    FirehoseClientBuilder builder =
        FirehoseClient.builder().region(Region.of(properties.getRegion()));
    if (StringUtils.hasText(properties.getEndpoint())) {
      builder.endpointOverride(URI.create(properties.getEndpoint()));
    }
    if (StringUtils.hasText(properties.getAccessKey())
        && StringUtils.hasText(properties.getSecretKey())) {
      builder.credentialsProvider(
          StaticCredentialsProvider.create(
              AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())));
    } else {
      builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
    }
    return builder.build();
  }
}
