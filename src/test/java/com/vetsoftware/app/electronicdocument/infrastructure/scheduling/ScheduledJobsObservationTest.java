package com.vetsoftware.app.electronicdocument.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics.Origin;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.application.usecase.DocumentTransmitter;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

class ScheduledJobsObservationTest {

    @Test
    void contingencyRetryReportsPartialFailureWithoutHighCardinalityTags() throws Exception {
        ElectronicDocumentRepository repository = mock(ElectronicDocumentRepository.class);
        DocumentTransmitter transmitter = mock(DocumentTransmitter.class);
        TransmissionLogPort transmissionLog = mock(TransmissionLogPort.class);
        ElectronicDocument successful = document(101L);
        ElectronicDocument failed = document(202L);
        when(repository.findByDianStatus(DianStatus.CONTINGENCIA))
                .thenReturn(List.of(successful, failed));
        when(transmissionLog.countAttempts(101L)).thenReturn(0);
        when(transmissionLog.countAttempts(202L)).thenReturn(0);
        when(transmitter.transmit(successful, Origin.RETRY)).thenReturn(successful);
        when(transmitter.transmit(failed, Origin.RETRY))
                .thenThrow(new IllegalStateException("provider unavailable"));
        ObservationCapture capture = new ObservationCapture();

        ContingencyRetryJob job = new ContingencyRetryJob(repository, transmitter, transmissionLog,
                capture.telemetry(), 12, 48);

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
        DocumentTransmitter transmitter = mock(DocumentTransmitter.class);
        when(repository.findByDianStatus(DianStatus.PENDIENTE)).thenReturn(List.of());
        ObservationCapture capture = new ObservationCapture();

        PendingReconciliationJob job = new PendingReconciliationJob(repository, transmitter,
                capture.telemetry());

        runAsSpringScheduledTask(job, "reconcilePending", capture.registry());

        Observation.Context context = capture.onlyContext();
        assertThat(context.getName()).isEqualTo("tasks.scheduled.execution");
        assertThat(context.getParentObservation()).isNull();
        assertThat(tag(context, "outcome")).isEqualTo("SUCCESS");
        assertThat(tag(context, "job.name")).isEqualTo("dian.pending.reconciliation");
        assertThat(tag(context, "job.outcome")).isEqualTo("no_work");
        verifyNoInteractions(transmitter);
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
