package com.vetsoftware.app.infrastructure.audit.outbox;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AuditOutboxPropertiesTest {

    @Test
    void rejectsBatchLargerThanFirehoseLimit() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setBatchSize(501);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("entre 1 y 500");
    }

    @Test
    void requiresStreamWhenPublisherIsEnabled() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setPublisherEnabled(true);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("delivery-stream-name");
    }

    @Test
    void rejectsIncompleteStaticCredentials() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setAccessKey("test");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deben configurarse juntos");
    }
}
