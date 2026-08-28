package com.vetsoftware.app.infrastructure.token;

import com.vetsoftware.app.infrastructure.observability.ScheduledJobCatalog;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.token.TokenCleanupMetrics.PurgedTokens;
import java.time.LocalDateTime;
import java.util.function.IntSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "vetsoftware.token-cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
final class TokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupJob.class);

    private final TokenCleanupRepository repository;
    private final TokenCleanupProperties properties;
    private final TokenCleanupMetrics metrics;
    private final ScheduledJobTelemetry telemetry;

    TokenCleanupJob(TokenCleanupRepository repository, TokenCleanupProperties properties,
            TokenCleanupMetrics metrics, ScheduledJobTelemetry telemetry) {
        properties.validate();
        this.repository = repository;
        this.properties = properties;
        this.metrics = metrics;
        this.telemetry = telemetry;
    }

    @Scheduled(cron = "${vetsoftware.token-cleanup.cron:0 20 * * * *}", zone = ScheduledJobCatalog.ZONE)
    void cleanup() {
        telemetry.observe(ScheduledJobCatalog.SECURITY_TOKENS_CLEANUP, this::cleanupTokens);
    }

    ScheduledJobTelemetry.Outcome cleanupTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minus(properties.getRetention());
        int batchSize = properties.getBatchSize();

        PurgedTokens purged = new PurgedTokens(
                drain(() -> repository.purgeRefreshTokens(cutoff, batchSize)),
                drain(() -> repository.purgeEmailVerificationTokens(cutoff, batchSize)),
                drain(() -> repository.purgePasswordResetTokens(cutoff, batchSize)));
        TokenCleanupRepository.TokenCounts rows = repository.countRows();
        metrics.record(rows, purged);

        if (purged.total() > 0) {
            log.info(
                    "Tokens depurados; refresh={}, verificación={}, restablecimiento={}, restantes={}",
                    purged.refresh(), purged.emailVerification(), purged.passwordReset(),
                    rows.total());
        }
        if (rows.anyAbove(properties.getGrowthWarningThreshold())) {
            log.warn(
                    "Crecimiento anormal de tokens; refresh={}, verificación={}, restablecimiento={},"
                            + " umbral={}",
                    rows.refresh(), rows.emailVerification(), rows.passwordReset(),
                    properties.getGrowthWarningThreshold());
        }

        return purged.total() == 0
                ? ScheduledJobTelemetry.Outcome.NO_WORK
                : ScheduledJobTelemetry.Outcome.SUCCESS;
    }

    private int drain(IntSupplier purgeBatch) {
        int total = 0;
        for (int batch = 0; batch < properties.getMaxBatchesPerRun(); batch++) {
            int deleted = purgeBatch.getAsInt();
            total += deleted;
            if (deleted < properties.getBatchSize()) {
                break;
            }
        }
        return total;
    }
}
