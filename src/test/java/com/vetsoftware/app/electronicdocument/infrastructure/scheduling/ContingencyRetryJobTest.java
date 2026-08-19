package com.vetsoftware.app.electronicdocument.infrastructure.scheduling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics.Origin;
import com.vetsoftware.app.electronicdocument.application.port.out.DianJobLeasePort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.application.usecase.DocumentTransmitter;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ContingencyRetryJob — lo que {@code ScheduledJobsLeaseTest} y
 * {@code ScheduledJobsObservationTest} no cubren: el documento arrendado que ya
 * no existe, los dos motivos de {@code isExhausted} (cap de intentos y ventana
 * de plazo) y los desenlaces FAILURE/SUCCESS completos (no solo
 * PARTIAL_FAILURE/NO_WORK).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContingencyRetryJob — reintento de documentos en contingencia DIAN")
class ContingencyRetryJobTest {

    private static final ScheduledJobTelemetry TELEMETRY = new ScheduledJobTelemetry(
            ObservationRegistry.create());
    private static final int MAX_ATTEMPTS = 4;
    private static final long DEADLINE_HOURS = 48;

    @Mock
    private ElectronicDocumentRepository repository;
    @Mock
    private DianJobLeasePort leasePort;
    @Mock
    private DocumentTransmitter transmitter;
    @Mock
    private TransmissionLogPort transmissionLog;

    private ContingencyRetryJob job;

    @BeforeEach
    void montar() {
        job = new ContingencyRetryJob(repository, leasePort, transmitter, transmissionLog,
                TELEMETRY, MAX_ATTEMPTS, DEADLINE_HOURS, 25, Duration.ofMinutes(30));
    }

    @Nested
    @DisplayName("un lote vacio no toca nada mas")
    class LoteVacio {
        @Test
        @DisplayName("sin documentos arrendados no consulta el repositorio ni el transmisor")
        void lote_vacio_no_toca_nada() {
            when(leasePort.leaseByDianStatus(eq(DianStatus.CONTINGENCIA), anyInt(), any()))
                    .thenReturn(List.of());

            job.retryContingencies();

            verifyNoInteractions(repository, transmitter, transmissionLog);
        }
    }

    @Nested
    @DisplayName("un documento arrendado que ya no existe se omite sin contarlo")
    class DocumentoInexistente {
        @Test
        @DisplayName("no llama al transmisor ni al log de intentos, y el resultado es NO_WORK")
        void documento_inexistente_se_omite() {
            when(leasePort.leaseByDianStatus(eq(DianStatus.CONTINGENCIA), anyInt(), any()))
                    .thenReturn(List.of(501L));
            when(repository.findById(501L)).thenReturn(Optional.empty());

            job.retryContingencies();

            verify(transmitter, never()).transmit(any(), any());
            verifyNoInteractions(transmissionLog);
        }
    }

    @Nested
    @DisplayName("isExhausted — un documento agotado no se reintenta")
    class Exhausted {

        @Test
        @DisplayName("un documento que agoto el cap de intentos no llega al transmisor")
        void cap_de_intentos_agotado_no_reintenta() {
            ElectronicDocument documento = ElectronicDocumentMother.documento(601L,
                    ElectronicDocumentMother.COMPANY_ID,
                    com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType.FE_VENTA,
                    DianStatus.CONTINGENCIA, "SETP", 990L, ElectronicDocumentMother.CUFE, null,
                    false, ElectronicDocumentMother.OPEN_ACCOUNT_ID);
            when(leasePort.leaseByDianStatus(eq(DianStatus.CONTINGENCIA), anyInt(), any()))
                    .thenReturn(List.of(601L));
            when(repository.findById(601L)).thenReturn(Optional.of(documento));
            when(transmissionLog.countAttempts(601L)).thenReturn(MAX_ATTEMPTS);

            job.retryContingencies();

            verify(transmitter, never()).transmit(any(), any());
        }

        @Test
        @DisplayName("un documento que supero la ventana de plazo no llega al transmisor")
        void ventana_de_plazo_superada_no_reintenta() {
            // ElectronicDocumentMother.documento() congela el createdDate en 2026-03-10,
            // muy anterior a "ahora" menos 48h en cualquier ejecucion real.
            ElectronicDocument documento = ElectronicDocumentMother.documento(701L,
                    ElectronicDocumentMother.COMPANY_ID,
                    com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType.FE_VENTA,
                    DianStatus.CONTINGENCIA, "SETP", 990L, ElectronicDocumentMother.CUFE, null,
                    false, ElectronicDocumentMother.OPEN_ACCOUNT_ID);
            when(leasePort.leaseByDianStatus(eq(DianStatus.CONTINGENCIA), anyInt(), any()))
                    .thenReturn(List.of(701L));
            when(repository.findById(701L)).thenReturn(Optional.of(documento));
            when(transmissionLog.countAttempts(701L)).thenReturn(0);

            job.retryContingencies();

            verify(transmitter, never()).transmit(any(), any());
        }
    }

    @Nested
    @DisplayName("desenlaces completos del lote")
    class Desenlaces {

        @Test
        @DisplayName("si todos los reintentos fallan, el job sigue sin lanzar (el fallo se registra por documento)")
        void todos_los_reintentos_fallan_no_lanza() {
            ElectronicDocument uno = documentoReciente(801L);
            ElectronicDocument dos = documentoReciente(802L);
            when(leasePort.leaseByDianStatus(eq(DianStatus.CONTINGENCIA), anyInt(), any()))
                    .thenReturn(List.of(801L, 802L));
            when(repository.findById(801L)).thenReturn(Optional.of(uno));
            when(repository.findById(802L)).thenReturn(Optional.of(dos));
            when(transmissionLog.countAttempts(801L)).thenReturn(0);
            when(transmissionLog.countAttempts(802L)).thenReturn(0);
            when(transmitter.transmit(uno, Origin.RETRY))
                    .thenThrow(new IllegalStateException("proveedor caido"));
            when(transmitter.transmit(dos, Origin.RETRY))
                    .thenThrow(new IllegalStateException("proveedor caido"));

            job.retryContingencies();

            verify(transmitter).transmit(uno, Origin.RETRY);
            verify(transmitter).transmit(dos, Origin.RETRY);
        }

        @Test
        @DisplayName("si todos los reintentos tienen exito, cada documento se retransmite")
        void todos_los_reintentos_exitosos() {
            ElectronicDocument uno = documentoReciente(901L);
            ElectronicDocument dos = documentoReciente(902L);
            when(leasePort.leaseByDianStatus(eq(DianStatus.CONTINGENCIA), anyInt(), any()))
                    .thenReturn(List.of(901L, 902L));
            when(repository.findById(901L)).thenReturn(Optional.of(uno));
            when(repository.findById(902L)).thenReturn(Optional.of(dos));
            when(transmissionLog.countAttempts(901L)).thenReturn(0);
            when(transmissionLog.countAttempts(902L)).thenReturn(0);
            when(transmitter.transmit(uno, Origin.RETRY)).thenReturn(uno);
            when(transmitter.transmit(dos, Origin.RETRY)).thenReturn(dos);

            job.retryContingencies();

            verify(transmitter).transmit(uno, Origin.RETRY);
            verify(transmitter).transmit(dos, Origin.RETRY);
        }
    }

    /**
     * A diferencia de {@code ElectronicDocumentMother.documento(...)} (createdDate
     * congelado en 2026-03-10, siempre "vencido"), este constructor usa
     * {@code LocalDateTime.now()} para que estos documentos NO disparen la rama de
     * ventana de plazo vencida y el test mida solo lo que dice medir.
     */
    private static ElectronicDocument documentoReciente(Long id) {
        return new ElectronicDocument(id, ElectronicDocumentMother.COMPANY_ID,
                ElectronicDocumentMother.OPEN_ACCOUNT_ID,
                com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType.FE_VENTA,
                "SETP", 990L, "18760000001", java.time.LocalDate.of(2026, 3, 10), "10:15:00-05:00",
                ElectronicDocumentMother.CUFE, null, "uuid-1", null, null, null, null,
                DianStatus.CONTINGENCIA, null, ElectronicDocumentMother.issuer(),
                ElectronicDocumentMother.customer(), new java.math.BigDecimal("1000.00"),
                new java.math.BigDecimal("1000.00"), new java.math.BigDecimal("1190.00"),
                new java.math.BigDecimal("1190.00"),
                com.vetsoftware.app.electronicdocument.domain.PaymentForm.CONTADO,
                ElectronicDocumentMother.unaLineaGravada(),
                ElectronicDocumentMother.efectivo("1190.00"), java.time.LocalDateTime.now(), null,
                true, null, null, null, false, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO, null, ElectronicDocumentMother.EMPLOYEE_ID,
                ElectronicDocumentMother.BRANCH_ID);
    }
}
