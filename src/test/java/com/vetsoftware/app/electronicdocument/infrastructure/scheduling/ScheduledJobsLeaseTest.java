package com.vetsoftware.app.electronicdocument.infrastructure.scheduling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics.Origin;
import com.vetsoftware.app.electronicdocument.application.port.out.DianJobLeasePort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.application.usecase.DeliverElectronicDocumentService;
import com.vetsoftware.app.electronicdocument.application.usecase.DocumentTransmitter;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * BE-04: los jobs de DIAN corren en todas las réplicas a la vez. Lo que impide
 * que retransmitan el mismo documento N veces es que cada una procese
 * únicamente el lote que reclamó.
 *
 * <p>
 * Por eso lo que se fija aquí no es solo que se use el lease, sino que ya
 * <b>no</b> se consulte la lista global por estado: volver a
 * {@code findByDianStatus} reintroduce el fallo entero aunque el lease siga en
 * su sitio.
 */
class ScheduledJobsLeaseTest {

    private static final ScheduledJobTelemetry TELEMETRY = new ScheduledJobTelemetry(
            ObservationRegistry.create());

    @Test
    void contingency_retry_solo_procesa_el_lote_reclamado() {
        ElectronicDocumentRepository repository = mock(ElectronicDocumentRepository.class);
        DianJobLeasePort leasePort = mock(DianJobLeasePort.class);
        DocumentTransmitter transmitter = mock(DocumentTransmitter.class);
        TransmissionLogPort transmissionLog = mock(TransmissionLogPort.class);
        ElectronicDocument mine = document(101L);
        when(leasePort.leaseByDianStatus(eq(DianStatus.CONTINGENCIA), anyInt(), any()))
                .thenReturn(List.of(101L));
        when(repository.findById(101L)).thenReturn(Optional.of(mine));
        when(transmissionLog.countAttempts(101L)).thenReturn(0);

        new ContingencyRetryJob(repository, leasePort, transmitter, transmissionLog, TELEMETRY, 4,
                48, 25, Duration.ofMinutes(30)).retryContingencies();

        verify(transmitter).transmit(mine, Origin.RETRY);
        verify(repository, never()).findByDianStatus(any());
    }

    @Test
    void reconciliacion_solo_procesa_el_lote_reclamado() {
        ElectronicDocumentRepository repository = mock(ElectronicDocumentRepository.class);
        DianJobLeasePort leasePort = mock(DianJobLeasePort.class);
        DocumentTransmitter transmitter = mock(DocumentTransmitter.class);
        ElectronicDocument mine = document(303L);
        when(leasePort.leaseByDianStatus(eq(DianStatus.PENDIENTE), anyInt(), any()))
                .thenReturn(List.of(303L));
        when(repository.findById(303L)).thenReturn(Optional.of(mine));

        new PendingReconciliationJob(repository, leasePort, transmitter, TELEMETRY, 50,
                Duration.ofMinutes(15)).reconcilePending();

        verify(transmitter).reconcile(mine);
        verify(repository, never()).findByDianStatus(any());
    }

    /**
     * Un documento reclamado por otra réplica no aparece en el lote, así que no se
     * toca.
     */
    @Test
    void lo_arrendado_por_otra_replica_no_se_procesa() {
        ElectronicDocumentRepository repository = mock(ElectronicDocumentRepository.class);
        DianJobLeasePort leasePort = mock(DianJobLeasePort.class);
        DocumentTransmitter transmitter = mock(DocumentTransmitter.class);
        when(leasePort.leaseByDianStatus(eq(DianStatus.PENDIENTE), anyInt(), any()))
                .thenReturn(List.of());

        new PendingReconciliationJob(repository, leasePort, transmitter, TELEMETRY, 50,
                Duration.ofMinutes(15)).reconcilePending();

        verify(transmitter, never()).reconcile(any());
        verify(repository, never()).findById(any());
    }

    /**
     * Issue #204. La re-entrega tambien corre en todas las replicas: sin arriendo,
     * N replicas generarian N veces el PDF de la misma factura y el cliente
     * recibiria N correos con su factura adjunta.
     */
    @Test
    void reentrega_solo_procesa_el_lote_reclamado() {
        ElectronicDocumentRepository repository = mock(ElectronicDocumentRepository.class);
        DianJobLeasePort leasePort = mock(DianJobLeasePort.class);
        DeliverElectronicDocumentService deliverService = mock(
                DeliverElectronicDocumentService.class);
        ElectronicDocument mine = mock(ElectronicDocument.class);
        when(mine.getCreatedDate()).thenReturn(LocalDateTime.now());
        when(leasePort.leaseUndeliveredValidated(anyInt(), any())).thenReturn(List.of(404L));
        when(repository.findById(404L)).thenReturn(Optional.of(mine));

        new DeliveryRetryJob(repository, leasePort, deliverService, TELEMETRY,
                Clock.systemDefaultZone(), 72, 25, Duration.ofMinutes(15)).retryDeliveries();

        verify(deliverService).deliverIfValidated(mine);
        verify(repository, never()).findByDianStatus(any());
    }

    private static ElectronicDocument document(long id) {
        ElectronicDocument document = mock(ElectronicDocument.class);
        when(document.getId()).thenReturn(id);
        return document;
    }
}
