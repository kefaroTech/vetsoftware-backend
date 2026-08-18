package com.vetsoftware.app.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;
import org.mockito.ArgumentCaptor;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.token.TokenCleanupMetrics.PurgedTokens;
import com.vetsoftware.app.infrastructure.token.TokenCleanupRepository.TokenCounts;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class TokenCleanupJobTest {

    private final TokenCleanupRepository repository = mock(TokenCleanupRepository.class);
    private final TokenCleanupMetrics metrics = mock(TokenCleanupMetrics.class);
    private final ScheduledJobTelemetry telemetry = mock(ScheduledJobTelemetry.class);
    private final TokenCleanupProperties properties = new TokenCleanupProperties();
    private TokenCleanupJob job;

    @BeforeEach
    void setUp() {
        properties.setRetention(Duration.ofDays(7));
        properties.setBatchSize(100);
        properties.setMaxBatchesPerRun(3);
        properties.setGrowthWarningThreshold(50_000);
        job = new TokenCleanupJob(repository, properties, metrics, telemetry);
    }

    @Test
    void purgesEachTableInBoundedBatchesAndReportsRemainingRows() {
        when(repository.purgeRefreshTokens(any(LocalDateTime.class), eq(100))).thenReturn(100, 20);
        when(repository.purgeEmailVerificationTokens(any(LocalDateTime.class), eq(100)))
                .thenReturn(0);
        when(repository.purgePasswordResetTokens(any(LocalDateTime.class), eq(100))).thenReturn(100,
                100, 100);
        TokenCounts remaining = new TokenCounts(5, 6, 7);
        when(repository.countRows()).thenReturn(remaining);

        ScheduledJobTelemetry.Outcome outcome = job.cleanupTokens();

        assertThat(outcome).isEqualTo(ScheduledJobTelemetry.Outcome.SUCCESS);
        verify(repository, times(2)).purgeRefreshTokens(any(LocalDateTime.class), eq(100));
        verify(repository).purgeEmailVerificationTokens(any(LocalDateTime.class), eq(100));
        verify(repository, times(3)).purgePasswordResetTokens(any(LocalDateTime.class), eq(100));
        verify(metrics).record(remaining, new PurgedTokens(120, 0, 300));
    }

    @Test
    void returnsNoWorkAndRemainsIdempotentWhenNothingIsEligible() {
        when(repository.countRows()).thenReturn(new TokenCounts(1, 2, 3));

        assertThat(job.cleanupTokens()).isEqualTo(ScheduledJobTelemetry.Outcome.NO_WORK);
        assertThat(job.cleanupTokens()).isEqualTo(ScheduledJobTelemetry.Outcome.NO_WORK);

        verify(repository, times(2)).purgeRefreshTokens(any(LocalDateTime.class), eq(100));
        verify(repository, times(2)).purgeEmailVerificationTokens(any(LocalDateTime.class),
                eq(100));
        verify(repository, times(2)).purgePasswordResetTokens(any(LocalDateTime.class), eq(100));
    }

    @Test
    @DisplayName("advierte cuando el número de filas restantes supera el umbral de crecimiento")
    void warnsWhenRowCountExceedsTheGrowthThreshold() {
        Logger logger = (Logger) LoggerFactory.getLogger(TokenCleanupJob.class);
        ListAppender<ILoggingEvent> sink = new ListAppender<>();
        sink.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        sink.start();
        logger.addAppender(sink);
        when(repository.countRows()).thenReturn(new TokenCounts(60_000, 0, 0));

        job.cleanupTokens();

        assertThat(sink.list).anyMatch(
                event -> event.getFormattedMessage().contains("Crecimiento anormal de tokens"));
        logger.detachAppender(sink);
    }

    @Test
    @DisplayName("cleanup delega en la telemetría del job programado con el nombre esperado")
    void cleanup_delega_en_la_telemetria_del_job_programado() {
        when(repository.countRows()).thenReturn(new TokenCounts(1, 2, 3));

        job.cleanup();

        ArgumentCaptor<Supplier<ScheduledJobTelemetry.Outcome>> actionCaptor = ArgumentCaptor
                .forClass(Supplier.class);
        verify(telemetry).observe(eq("security.tokens.cleanup"), actionCaptor.capture());
        assertThat(actionCaptor.getValue().get()).isEqualTo(ScheduledJobTelemetry.Outcome.NO_WORK);
    }
}
