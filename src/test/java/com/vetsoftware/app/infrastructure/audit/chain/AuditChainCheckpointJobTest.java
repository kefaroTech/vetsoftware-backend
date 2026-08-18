package com.vetsoftware.app.infrastructure.audit.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.audit.outbox.AuditEventStore;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditChainCheckpointJob")
class AuditChainCheckpointJobTest {

    private static final String HASH = AuditChainHash.payloadHash("checkpoint");

    @Mock
    private AuditChainRepository repository;
    @Mock
    private AuditEventStore eventStore;
    @Mock
    private AuditChainMetrics metrics;
    @Mock
    private ScheduledJobTelemetry telemetry;

    private AuditChainCheckpointJob job;

    @BeforeEach
    void setUp() {
        job = new AuditChainCheckpointJob(repository, eventStore, metrics, telemetry);
    }

    @Nested
    @DisplayName("emitCheckpoint")
    class EmitCheckpoint {

        @Test
        @DisplayName("no hace nada cuando la cadena no tiene eventos todavia")
        void no_hace_nada_cuando_la_cadena_no_tiene_eventos() {
            when(repository.head()).thenReturn(new AuditChainRepository.Head(0L, HASH, 0L));

            ScheduledJobTelemetry.Outcome outcome = job.emitCheckpoint();

            assertThat(outcome).isEqualTo(ScheduledJobTelemetry.Outcome.NO_WORK);
            verifyNoInteractions(eventStore, metrics);
            verify(repository, never()).markCheckpoint(any(Long.class), any(Instant.class));
        }

        @Test
        @DisplayName("no hace nada cuando ya se ancló la ultima posicion")
        void no_hace_nada_cuando_ya_se_anclo_la_ultima_posicion() {
            when(repository.head()).thenReturn(new AuditChainRepository.Head(9L, HASH, 9L));

            ScheduledJobTelemetry.Outcome outcome = job.emitCheckpoint();

            assertThat(outcome).isEqualTo(ScheduledJobTelemetry.Outcome.NO_WORK);
            verifyNoInteractions(eventStore, metrics);
        }

        @Test
        @DisplayName("emite el checkpoint, marca la cadena y actualiza la metrica")
        void emite_el_checkpoint_y_marca_la_cadena() {
            when(repository.head()).thenReturn(new AuditChainRepository.Head(10L, HASH, 3L));

            ScheduledJobTelemetry.Outcome outcome = job.emitCheckpoint();

            assertThat(outcome).isEqualTo(ScheduledJobTelemetry.Outcome.SUCCESS);

            ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor
                    .forClass(Map.class);
            verify(eventStore).append(eq("audit_chain_checkpoint"), eq("SUCCESS"),
                    attributesCaptor.capture());
            assertThat(attributesCaptor.getValue()).isEqualTo(Map.of("chain.sequence", 10L,
                    "chain.hash", HASH, "chain.previousCheckpointSequence", 3L));

            verify(repository).markCheckpoint(eq(10L), any(Instant.class));
            verify(metrics).checkpointed(10L);
        }
    }

    @Nested
    @DisplayName("checkpoint")
    class Checkpoint {

        @Test
        @DisplayName("delega en la telemetria con el nombre del job")
        void delega_en_la_telemetria_con_el_nombre_del_job() {
            when(repository.head()).thenReturn(new AuditChainRepository.Head(0L, HASH, 0L));

            job.checkpoint();

            ArgumentCaptor<Supplier<ScheduledJobTelemetry.Outcome>> captor = ArgumentCaptor
                    .forClass(Supplier.class);
            verify(telemetry).observe(eq("audit.chain.checkpoint"), captor.capture());
            assertThat(captor.getValue().get()).isEqualTo(ScheduledJobTelemetry.Outcome.NO_WORK);
        }
    }
}
