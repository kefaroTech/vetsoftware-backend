package com.vetsoftware.app.infrastructure.audit.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditOutboxCleanupJob")
class AuditOutboxCleanupJobTest {

    @Mock
    private AuditOutboxRepository repository;
    @Mock
    private ScheduledJobTelemetry telemetry;

    private AuditOutboxProperties properties;
    private AuditOutboxCleanupJob job;

    @BeforeEach
    void setUp() {
        properties = new AuditOutboxProperties();
        properties.setRetention(Duration.ofDays(7));
        properties.setCleanupBatchSize(500);
        job = new AuditOutboxCleanupJob(repository, properties, telemetry);
    }

    @Test
    @DisplayName("depura publicados vencidos usando la retencion configurada")
    void depura_publicados_vencidos_usando_la_retencion_configurada() {
        when(repository.deletePublishedBefore(any(Instant.class), eq(500))).thenReturn(120);

        job.cleanup();

        ArgumentCaptor<Supplier<ScheduledJobTelemetry.Outcome>> supplierCaptor = ArgumentCaptor
                .forClass(Supplier.class);
        verify(telemetry).observe(eq("audit.outbox.cleanup"), supplierCaptor.capture());
        assertThat(supplierCaptor.getValue().get())
                .isEqualTo(ScheduledJobTelemetry.Outcome.SUCCESS);

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deletePublishedBefore(cutoffCaptor.capture(), eq(500));
        assertThat(cutoffCaptor.getValue()).isCloseTo(Instant.now().minus(Duration.ofDays(7)),
                within(5, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("reporta NO_WORK cuando no hay publicados vencidos que depurar")
    void reporta_no_work_cuando_no_hay_publicados_vencidos() {
        when(repository.deletePublishedBefore(any(Instant.class), eq(500))).thenReturn(0);

        job.cleanup();

        ArgumentCaptor<Supplier<ScheduledJobTelemetry.Outcome>> supplierCaptor = ArgumentCaptor
                .forClass(Supplier.class);
        verify(telemetry).observe(eq("audit.outbox.cleanup"), supplierCaptor.capture());
        assertThat(supplierCaptor.getValue().get())
                .isEqualTo(ScheduledJobTelemetry.Outcome.NO_WORK);
    }
}
