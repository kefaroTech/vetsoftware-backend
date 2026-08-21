package com.vetsoftware.app.electronicdocument.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
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
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

class ScheduledJobsObservationTest {

    @Test
    void contingencyRetryReportsPartialFailureWithoutHighCardinalityTags() throws Exception {
        ElectronicDocumentRepository repository = mock(ElectronicDocumentRepository.class);
        DianJobLeasePort leasePort = mock(DianJobLeasePort.class);
        DocumentTransmitter transmitter = mock(DocumentTransmitter.class);
        TransmissionLogPort transmissionLog = mock(TransmissionLogPort.class);
        ElectronicDocument successful = document(101L);
        ElectronicDocument failed = document(202L);
        when(leasePort.leaseByDianStatus(eq(DianStatus.CONTINGENCIA), anyInt(), any()))
                .thenReturn(List.of(101L, 202L));
        when(repository.findById(101L)).thenReturn(Optional.of(successful));
        when(repository.findById(202L)).thenReturn(Optional.of(failed));
        when(transmissionLog.countAttempts(101L)).thenReturn(0);
        when(transmissionLog.countAttempts(202L)).thenReturn(0);
        when(transmitter.transmit(successful, Origin.RETRY)).thenReturn(successful);
        when(transmitter.transmit(failed, Origin.RETRY))
                .thenThrow(new IllegalStateException("provider unavailable"));
        ObservationCapture capture = new ObservationCapture();

        ContingencyRetryJob job = new ContingencyRetryJob(repository, leasePort, transmitter,
                transmissionLog, capture.telemetry(), 4, 48, 25, Duration.ofMinutes(30));

        runAsSpringScheduledTask(job, "retryContingencies", capture.registry());

        Observation.Context context = capture.onlyContext();
        assertThat(context.getName()).isEqualTo("tasks.scheduled.execution");
        assertThat(context.getParentObservation()).isNull();
        assertThat(tag(context, "code.function")).isEqualTo("retryContingencies");
        assertThat(tag(context, "job.name")).isEqualTo("dian.contingency.retry");
        assertThat(tag(context, "job.outcome")).isEqualTo("partial_failure");
        assertThat(context.getLowCardinalityKeyValues())
                .noneMatch(keyValue -> keyValue.getKey().contains("document")
                        || keyValue.getValue().equals("101") || keyValue.getValue().equals("202"));
    }

    @Test
    void pendingReconciliationReportsNoWork() throws Exception {
        ElectronicDocumentRepository repository = mock(ElectronicDocumentRepository.class);
        DianJobLeasePort leasePort = mock(DianJobLeasePort.class);
        DocumentTransmitter transmitter = mock(DocumentTransmitter.class);
        when(leasePort.leaseByDianStatus(eq(DianStatus.PENDIENTE), anyInt(), any()))
                .thenReturn(List.of());
        ObservationCapture capture = new ObservationCapture();

        PendingReconciliationJob job = new PendingReconciliationJob(repository, leasePort,
                transmitter, capture.telemetry(), 50, Duration.ofMinutes(15));

        runAsSpringScheduledTask(job, "reconcilePending", capture.registry());

        Observation.Context context = capture.onlyContext();
        assertThat(context.getName()).isEqualTo("tasks.scheduled.execution");
        assertThat(context.getParentObservation()).isNull();
        assertThat(tag(context, "outcome")).isEqualTo("SUCCESS");
        assertThat(tag(context, "job.name")).isEqualTo("dian.pending.reconciliation");
        assertThat(tag(context, "job.outcome")).isEqualTo("no_work");
        verifyNoInteractions(transmitter);
    }

    /**
     * Issue #204. El job de re-entrega trabaja sobre documentos identificados por
     * id: publicarlos como etiqueta seria una serie por factura. Se fija ademas su
     * {@code job.name}, que es la unica via por la que un tablero puede distinguir
     * esta poblacion de la de contingencia.
     */
    @Test
    void deliveryRetryReportsPartialFailureWithoutHighCardinalityTags() throws Exception {
        ElectronicDocumentRepository repository = mock(ElectronicDocumentRepository.class);
        DianJobLeasePort leasePort = mock(DianJobLeasePort.class);
        DeliverElectronicDocumentService deliverService = mock(
                DeliverElectronicDocumentService.class);
        ElectronicDocument entregado = recentDocument();
        ElectronicDocument fallido = recentDocument();
        when(leasePort.leaseUndeliveredValidated(anyInt(), any())).thenReturn(List.of(101L, 202L));
        when(repository.findById(101L)).thenReturn(Optional.of(entregado));
        when(repository.findById(202L)).thenReturn(Optional.of(fallido));
        doThrow(new IllegalStateException("S3 no responde")).when(deliverService)
                .deliverIfValidated(fallido);
        ObservationCapture capture = new ObservationCapture();

        DeliveryRetryJob job = new DeliveryRetryJob(repository, leasePort, deliverService,
                capture.telemetry(), Clock.systemDefaultZone(), 72, 25, Duration.ofMinutes(15));

        runAsSpringScheduledTask(job, "retryDeliveries", capture.registry());

        Observation.Context context = capture.onlyContext();
        assertThat(context.getName()).isEqualTo("tasks.scheduled.execution");
        assertThat(tag(context, "job.name")).isEqualTo("dian.delivery.retry");
        assertThat(tag(context, "job.outcome")).isEqualTo("partial_failure");
        assertThat(context.getLowCardinalityKeyValues())
                .noneMatch(keyValue -> keyValue.getKey().contains("document")
                        || keyValue.getValue().equals("101") || keyValue.getValue().equals("202"));
    }

    private static ElectronicDocument recentDocument() {
        ElectronicDocument document = mock(ElectronicDocument.class);
        when(document.getCreatedDate()).thenReturn(LocalDateTime.now());
        return document;
    }

    private static void runAsSpringScheduledTask(Object target, String methodName,
            ObservationRegistry registry) throws NoSuchMethodException {
        new ScheduledMethodRunnable(target, target.getClass().getMethod(methodName), null,
                () -> registry).run();
    }

    private static ElectronicDocument document(long id) {
        ElectronicDocument document = mock(ElectronicDocument.class);
        when(document.getId()).thenReturn(id);
        return document;
    }

    private static String tag(Observation.Context context, String key) {
        return context.getLowCardinalityKeyValue(key).getValue();
    }

    private static final class ObservationCapture
            implements
                ObservationHandler<Observation.Context> {
        private final ObservationRegistry registry = ObservationRegistry.create();
        private final List<Observation.Context> stopped = new ArrayList<>();

        private ObservationCapture() {
            registry.observationConfig().observationHandler(this);
        }

        private ScheduledJobTelemetry telemetry() {
            return new ScheduledJobTelemetry(registry);
        }

        private ObservationRegistry registry() {
            return registry;
        }

        private Observation.Context onlyContext() {
            assertThat(stopped).hasSize(1);
            return stopped.getFirst();
        }

        @Override
        public void onStop(Observation.Context context) {
            stopped.add(context);
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }
    }
}
