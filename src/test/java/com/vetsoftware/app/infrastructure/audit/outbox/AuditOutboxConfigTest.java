package com.vetsoftware.app.infrastructure.audit.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void fails_fast_when_delivery_stream_placeholder_was_not_resolved() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setPublisherEnabled(true);
        properties.setRegion("us-east-1");
        // El Binder deja el placeholder literal cuando la variable no está definida.
        properties.setDeliveryStreamName("${AUDIT_FIREHOSE_DELIVERY_STREAM}");

        assertThatThrownBy(() -> new AuditOutboxConfig().auditFirehoseClient(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder sin resolver")
                .hasMessageContaining("AUDIT_OUTBOX_PUBLISHER_ENABLED=false");
    }
}
