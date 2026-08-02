package com.vetsoftware.app.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TokenCleanupPropertiesTest {

    @Test
    void rejectsNonPositiveRetention() {
        TokenCleanupProperties properties = new TokenCleanupProperties();
        properties.setRetention(Duration.ZERO);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("retention");
    }

    @Test
    void rejectsUnboundedBatchConfiguration() {
        TokenCleanupProperties properties = new TokenCleanupProperties();
        properties.setMaxBatchesPerRun(101);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-batches-per-run");
    }
}
