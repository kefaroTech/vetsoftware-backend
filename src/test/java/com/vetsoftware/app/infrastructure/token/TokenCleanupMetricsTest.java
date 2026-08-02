package com.vetsoftware.app.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.infrastructure.token.TokenCleanupMetrics.PurgedTokens;
import com.vetsoftware.app.infrastructure.token.TokenCleanupRepository.TokenCounts;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class TokenCleanupMetricsTest {

    @Test
    void exposesRowsPurgedTotalsAndConfiguredGrowthThreshold() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TokenCleanupProperties properties = new TokenCleanupProperties();
        properties.setGrowthWarningThreshold(12_345);
        TokenCleanupMetrics metrics = new TokenCleanupMetrics(registry, properties);

        metrics.record(new TokenCounts(10, 20, 30), new PurgedTokens(1, 2, 3));

        assertThat(registry.get(TokenCleanupMetrics.ROWS_METRIC).tag("token.type", "refresh")
                .gauge().value()).isEqualTo(10);
        assertThat(registry.get(TokenCleanupMetrics.ROWS_METRIC)
                .tag("token.type", "email_verification").gauge().value()).isEqualTo(20);
        assertThat(registry.get(TokenCleanupMetrics.PURGED_METRIC)
                .tag("token.type", "password_reset").counter().count()).isEqualTo(3);
        assertThat(registry.get(TokenCleanupMetrics.GROWTH_THRESHOLD_METRIC).gauge().value())
                .isEqualTo(12_345);
    }
}
