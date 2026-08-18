package com.vetsoftware.app.electronicdocument.infrastructure.scheduling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.DianJobLeasePort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
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
 * PendingReconciliationJob — lo que {@code ScheduledJobsLeaseTest} y
 * {@code ScheduledJobsObservationTest} no cubren: el documento arrendado que ya
 * no existe, una reconciliacion que lanza (se registra como fallo sin abortar
 * el lote) y los desenlaces SUCCESS/FAILURE completos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PendingReconciliationJob — reconciliacion de documentos PENDIENTE")
class PendingReconciliationJobTest {

    private static final ScheduledJobTelemetry TELEMETRY = new ScheduledJobTelemetry(
            ObservationRegistry.create());

    @Mock
    private ElectronicDocumentRepository repository;
    @Mock
    private DianJobLeasePort leasePort;
    @Mock
    private DocumentTransmitter transmitter;

    private PendingReconciliationJob job;

    @BeforeEach
    void montar() {
        job = new PendingReconciliationJob(repository, leasePort, transmitter, TELEMETRY, 50,
                Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("un documento arrendado que ya no existe se omite sin llamar al transmisor")
    void documento_inexistente_se_omite() {
        when(leasePort.leaseByDianStatus(eq(DianStatus.PENDIENTE), anyInt(), any()))
                .thenReturn(List.of(401L));
        when(repository.findById(401L)).thenReturn(Optional.empty());

        job.reconcilePending();

        verify(transmitter, never()).reconcile(any());
    }

    @Nested
    @DisplayName("desenlaces completos del lote")
    class Desenlaces {

        @Test
        @DisplayName("una reconciliacion que lanza se registra como fallo, sin abortar el job")
        void reconciliacion_que_lanza_no_aborta_el_job() {
            ElectronicDocument documento = ElectronicDocumentMother.facturaPendienteConId(301L);
            when(leasePort.leaseByDianStatus(eq(DianStatus.PENDIENTE), anyInt(), any()))
                    .thenReturn(List.of(301L));
            when(repository.findById(301L)).thenReturn(Optional.of(documento));
            when(transmitter.reconcile(documento))
                    .thenThrow(new IllegalStateException("proveedor no responde"));

            job.reconcilePending();

            verify(transmitter).reconcile(documento);
        }

        @Test
        @DisplayName("si todas las reconciliaciones tienen exito, cada documento se reconcilia")
        void todas_las_reconciliaciones_exitosas() {
            ElectronicDocument uno = ElectronicDocumentMother.facturaPendienteConId(401L);
            ElectronicDocument dos = ElectronicDocumentMother.facturaPendienteConId(402L);
            when(leasePort.leaseByDianStatus(eq(DianStatus.PENDIENTE), anyInt(), any()))
                    .thenReturn(List.of(401L, 402L));
            when(repository.findById(401L)).thenReturn(Optional.of(uno));
            when(repository.findById(402L)).thenReturn(Optional.of(dos));
            when(transmitter.reconcile(uno)).thenReturn(uno);
            when(transmitter.reconcile(dos)).thenReturn(dos);

            job.reconcilePending();

            verify(transmitter).reconcile(uno);
            verify(transmitter).reconcile(dos);
        }

        @Test
        @DisplayName("una mezcla de exito y fallo reconcilia igualmente cada documento del lote")
        void mezcla_de_exito_y_fallo_reconcilia_todo_el_lote() {
            ElectronicDocument exitoso = ElectronicDocumentMother.facturaPendienteConId(501L);
            ElectronicDocument fallido = ElectronicDocumentMother.facturaPendienteConId(502L);
            when(leasePort.leaseByDianStatus(eq(DianStatus.PENDIENTE), anyInt(), any()))
                    .thenReturn(List.of(501L, 502L));
            when(repository.findById(501L)).thenReturn(Optional.of(exitoso));
            when(repository.findById(502L)).thenReturn(Optional.of(fallido));
            when(transmitter.reconcile(exitoso)).thenReturn(exitoso);
            when(transmitter.reconcile(fallido)).thenThrow(new IllegalStateException("timeout"));

            job.reconcilePending();

            verify(transmitter).reconcile(exitoso);
            verify(transmitter).reconcile(fallido);
        }
    }

}
