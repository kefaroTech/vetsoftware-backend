package com.vetsoftware.app.infrastructure.audit.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.audit.outbox.AuditOutboxProperties;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import java.util.List;
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
@DisplayName("AuditChainVerificationJob")
class AuditChainVerificationJobTest {

    private static final String PAYLOAD_HASH_1 = AuditChainHash.payloadHash("evento-1");
    private static final String PAYLOAD_HASH_2 = AuditChainHash.payloadHash("evento-2");
    private static final String CHAIN_HASH_1 = AuditChainHash.chainHash(AuditChainHash.GENESIS_HASH,
            1, PAYLOAD_HASH_1);
    private static final String CHAIN_HASH_2 = AuditChainHash.chainHash(CHAIN_HASH_1, 2,
            PAYLOAD_HASH_2);

    @Mock
    private AuditChainRepository repository;
    @Mock
    private AuditChainMetrics metrics;
    @Mock
    private ScheduledJobTelemetry telemetry;

    private AuditChainVerificationJob job;

    @BeforeEach
    void setUp() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setVerifyBatchSize(50);
        job = new AuditChainVerificationJob(repository, metrics, telemetry, properties);
    }

    @Nested
    @DisplayName("verifySweep")
    class VerifySweep {

        @Test
        @DisplayName("no hace nada cuando no hay eslabones retenidos")
        void no_hace_nada_cuando_no_hay_eslabones_retenidos() {
            when(repository.linksAfter(0L, 50)).thenReturn(List.of());

            ScheduledJobTelemetry.Outcome outcome = job.verifySweep();

            assertThat(outcome).isEqualTo(ScheduledJobTelemetry.Outcome.NO_WORK);
            verifyNoInteractions(metrics);
        }

        @Test
        @DisplayName("una cadena intacta reporta SUCCESS y publica la ultima posicion verificada")
        void una_cadena_intacta_reporta_success() {
            AuditChainRepository.Link link1 = new AuditChainRepository.Link(1, "evento-1",
                    PAYLOAD_HASH_1, AuditChainHash.GENESIS_HASH, CHAIN_HASH_1);
            AuditChainRepository.Link link2 = new AuditChainRepository.Link(2, "evento-2",
                    PAYLOAD_HASH_2, CHAIN_HASH_1, CHAIN_HASH_2);
            when(repository.linksAfter(0L, 50)).thenReturn(List.of(link1, link2));
            when(repository.linksAfter(2L, 50)).thenReturn(List.of());

            ScheduledJobTelemetry.Outcome outcome = job.verifySweep();

            assertThat(outcome).isEqualTo(ScheduledJobTelemetry.Outcome.SUCCESS);
            ArgumentCaptor<AuditChainVerifier.Result> captor = ArgumentCaptor
                    .forClass(AuditChainVerifier.Result.class);
            verify(metrics).verified(captor.capture());
            assertThat(captor.getValue().intact()).isTrue();
            assertThat(captor.getValue().lastVerifiedSequence()).isEqualTo(2L);
        }

        @Test
        @DisplayName("un hueco en la cadena reporta FAILURE y detiene el barrido")
        void un_hueco_en_la_cadena_reporta_failure() {
            AuditChainRepository.Link link1 = new AuditChainRepository.Link(1, "evento-1",
                    PAYLOAD_HASH_1, AuditChainHash.GENESIS_HASH, CHAIN_HASH_1);
            AuditChainRepository.Link link3 = new AuditChainRepository.Link(3, "evento-3",
                    PAYLOAD_HASH_2, CHAIN_HASH_1, CHAIN_HASH_2);
            when(repository.linksAfter(0L, 50)).thenReturn(List.of(link1, link3));

            ScheduledJobTelemetry.Outcome outcome = job.verifySweep();

            assertThat(outcome).isEqualTo(ScheduledJobTelemetry.Outcome.FAILURE);
            ArgumentCaptor<AuditChainVerifier.Result> captor = ArgumentCaptor
                    .forClass(AuditChainVerifier.Result.class);
            verify(metrics).verified(captor.capture());
            assertThat(captor.getValue().intact()).isFalse();
            assertThat(captor.getValue().failureSequence()).isEqualTo(3L);
            verify(repository, never()).linksAfter(eq(1L), any(Integer.class));
        }

        @Test
        @DisplayName("recorre varios lotes hasta agotar la ventana retenida")
        void recorre_varios_lotes_hasta_agotar_la_ventana() {
            AuditChainRepository.Link link1 = new AuditChainRepository.Link(1, "evento-1",
                    PAYLOAD_HASH_1, AuditChainHash.GENESIS_HASH, CHAIN_HASH_1);
            AuditChainRepository.Link link2 = new AuditChainRepository.Link(2, "evento-2",
                    PAYLOAD_HASH_2, CHAIN_HASH_1, CHAIN_HASH_2);
            when(repository.linksAfter(0L, 50)).thenReturn(List.of(link1));
            when(repository.linksAfter(1L, 50)).thenReturn(List.of(link2));
            when(repository.linksAfter(2L, 50)).thenReturn(List.of());

            ScheduledJobTelemetry.Outcome outcome = job.verifySweep();

            assertThat(outcome).isEqualTo(ScheduledJobTelemetry.Outcome.SUCCESS);
            verify(repository, times(3)).linksAfter(any(Long.class), eq(50));
        }
    }

    @Nested
    @DisplayName("verify")
    class Verify {

        @Test
        @DisplayName("delega en la telemetria con el nombre del job")
        void delega_en_la_telemetria_con_el_nombre_del_job() {
            when(repository.linksAfter(0L, 50)).thenReturn(List.of());

            job.verify();

            ArgumentCaptor<Supplier<ScheduledJobTelemetry.Outcome>> captor = ArgumentCaptor
                    .forClass(Supplier.class);
            verify(telemetry).observe(eq("audit.chain.verify"), captor.capture());
            assertThat(captor.getValue().get()).isEqualTo(ScheduledJobTelemetry.Outcome.NO_WORK);
        }
    }
}
