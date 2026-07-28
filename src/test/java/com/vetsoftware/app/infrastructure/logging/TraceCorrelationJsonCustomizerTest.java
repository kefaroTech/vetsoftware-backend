package com.vetsoftware.app.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TraceCorrelationJsonCustomizerTest {

    @Test
    void recognizesOnlyRealLowercaseTraceAndSpanIdentifiers() {
        assertThat(TraceCorrelationJsonCustomizer.isTraceCorrelated(Map.of(
                "traceId", "21681c8c48fef44cf10671aec7948dfe",
                "spanId", "10671aec7948dfe0")))
                .isTrue();

        assertThat(TraceCorrelationJsonCustomizer.isTraceCorrelated(Map.of(
                "traceId", "untraced",
                "spanId", "untraced")))
                .isFalse();

        assertThat(TraceCorrelationJsonCustomizer.isTraceCorrelated(Map.of(
                "traceId", "21681C8C48FEF44CF10671AEC7948DFE",
                "spanId", "10671AEC7948DFE0")))
                .isFalse();

        assertThat(TraceCorrelationJsonCustomizer.isTraceCorrelated(Map.of(
                "traceId", "21681c8c48fef44cf10671aec7948dfe")))
                .isFalse();

        assertThat(TraceCorrelationJsonCustomizer.isTraceCorrelated(null))
                .isFalse();
    }
}
