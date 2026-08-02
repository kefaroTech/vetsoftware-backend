package com.vetsoftware.app.infrastructure.token;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
class TokenCleanupMetrics {

    static final String ROWS_METRIC = "vetsoftware.security.tokens.rows";
    static final String PURGED_METRIC = "vetsoftware.security.tokens.purged";
    static final String GROWTH_THRESHOLD_METRIC = "vetsoftware.security.tokens.growth.threshold";

    private final AtomicLong refreshRows = new AtomicLong();
    private final AtomicLong emailVerificationRows = new AtomicLong();
    private final AtomicLong passwordResetRows = new AtomicLong();
    private final Counter refreshPurged;
    private final Counter emailVerificationPurged;
    private final Counter passwordResetPurged;

    TokenCleanupMetrics(MeterRegistry registry, TokenCleanupProperties properties) {
        bindGauge(registry, refreshRows, "refresh");
        bindGauge(registry, emailVerificationRows, "email_verification");
        bindGauge(registry, passwordResetRows, "password_reset");
        Gauge.builder(GROWTH_THRESHOLD_METRIC, properties, TokenCleanupProperties::getGrowthWarningThreshold)
                .description("Umbral de alerta para filas de tokens de seguridad")
                .register(registry);
        refreshPurged = bindCounter(registry, "refresh");
        emailVerificationPurged = bindCounter(registry, "email_verification");
        passwordResetPurged = bindCounter(registry, "password_reset");
    }

    void record(TokenCleanupRepository.TokenCounts rows, PurgedTokens purged) {
        refreshRows.set(rows.refresh());
        emailVerificationRows.set(rows.emailVerification());
        passwordResetRows.set(rows.passwordReset());
        refreshPurged.increment(purged.refresh());
        emailVerificationPurged.increment(purged.emailVerification());
        passwordResetPurged.increment(purged.passwordReset());
    }

    private static void bindGauge(MeterRegistry registry, AtomicLong value, String tokenType) {
        Gauge.builder(ROWS_METRIC, value, AtomicLong::get)
                .description("Filas persistidas por tipo de token de seguridad")
                .tag("token.type", tokenType)
                .register(registry);
    }

    private static Counter bindCounter(MeterRegistry registry, String tokenType) {
        return Counter.builder(PURGED_METRIC)
                .description("Tokens de seguridad eliminados por la política de retención")
                .tag("token.type", tokenType)
                .register(registry);
    }

    record PurgedTokens(int refresh, int emailVerification, int passwordReset) {
        int total() {
            return refresh + emailVerification + passwordReset;
        }
    }
}
