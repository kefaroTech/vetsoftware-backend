package com.vetsoftware.app.infrastructure.audit.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.firehose.FirehoseClient;

class AuditOutboxConfigTest {

  @Test
  void buildsLocalStackClientWhenEndpointIsConfigured() {
    AuditOutboxProperties properties = new AuditOutboxProperties();
    properties.setPublisherEnabled(true);
    properties.setDeliveryStreamName("vetsoftware-audit-local");
    properties.setRegion("us-east-1");
    properties.setEndpoint("http://localhost:4566");
    properties.setAccessKey("test");
    properties.setSecretKey("test");

    try (FirehoseClient client = new AuditOutboxConfig().auditFirehoseClient(properties)) {
      assertThat(client.serviceClientConfiguration().endpointOverride())
          .contains(URI.create("http://localhost:4566"));
    }
  }
}
