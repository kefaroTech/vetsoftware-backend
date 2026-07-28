package com.vetsoftware.app.electronicdocument.infrastructure.scheduling;

import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.usecase.DocumentTransmitter;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reconcilia los documentos atascados en PENDIENTE consultando al proveedor su estado actual. Es el
 * respaldo ante webhooks perdidos de proveedores asíncronos (p. ej. MATIAS): si el webhook
 * {@code document.accepted/rejected} nunca llega, el documento quedaría PENDIENTE para siempre.
 *
 * <p>Cada documento se reconcilia en su propia transacción ({@link DocumentTransmitter#reconcile}); un
 * fallo no afecta a los demás. El job solo cierra los que el proveedor reporte ya como VALIDADO/RECHAZADO
 * (un proveedor sin polling devolvería vacío y sería no-op).
 */
@Component
public class PendingReconciliationJob {
    private static final Logger log = LoggerFactory.getLogger(PendingReconciliationJob.class);
    private static final String JOB_NAME = "dian.pending.reconciliation";

    private final ElectronicDocumentRepository repository;
    private final DocumentTransmitter transmitter;
    private final ScheduledJobTelemetry telemetry;

    public PendingReconciliationJob(ElectronicDocumentRepository repository,
                                    DocumentTransmitter transmitter,
                                    ScheduledJobTelemetry telemetry) {
        this.repository = repository;
        this.transmitter = transmitter;
        this.telemetry = telemetry;
    }

    @Scheduled(
            initialDelayString = "${dian.reconciliation.initial-delay-ms:120000}",
            fixedDelayString = "${dian.reconciliation.poll-delay-ms:600000}")
    public void reconcilePending() {
        telemetry.observe(JOB_NAME, this::executeReconciliation);
    }

    private Outcome executeReconciliation() {
        List<ElectronicDocument> pending = repository.findByDianStatus(DianStatus.PENDIENTE);
        if (pending.isEmpty()) return Outcome.NO_WORK;
        log.info("Reconciliando documento(s) PENDIENTE contra el proveedor DIAN: {} candidato(s)", pending.size());
        int failures = 0;
        for (ElectronicDocument document : pending) {
            try {
                transmitter.reconcile(document);
            } catch (Exception e) {
                failures++;
                log.warn("Reconciliación falló para documento {}: {}", document.getId(), e.getMessage());
            }
        }
        Outcome outcome = Outcome.from(pending.size(), failures);
        log.info("Reconciliación DIAN finalizada: intentado(s)={}, fallido(s)={}, resultado={}",
                pending.size(), failures, outcome.value());
        return outcome;
    }
}
