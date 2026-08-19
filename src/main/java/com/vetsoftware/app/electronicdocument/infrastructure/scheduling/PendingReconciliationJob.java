package com.vetsoftware.app.electronicdocument.infrastructure.scheduling;

import com.vetsoftware.app.electronicdocument.application.port.out.DianJobLeasePort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.usecase.DocumentTransmitter;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reconcilia los documentos atascados en PENDIENTE consultando al proveedor su
 * estado actual. Es el respaldo ante webhooks perdidos de proveedores
 * asíncronos (p. ej. MATIAS): si el webhook {@code
 * document.accepted/rejected} nunca llega, el documento quedaría PENDIENTE para
 * siempre.
 *
 * <p>
 * Cada documento se reconcilia en su propia transacción
 * ({@link DocumentTransmitter#reconcile}); un fallo no afecta a los demás. El
 * job solo cierra los que el proveedor reporte ya como VALIDADO/RECHAZADO (un
 * proveedor sin polling devolvería vacío y sería no-op).
 *
 * <p>
 * <b>El ciclo es de 12h</b> ({@code dian.reconciliation.poll-delay-ms}),
 * contadas desde que TERMINA la pasada anterior. Es el techo de cuanto puede
 * quedarse un documento en PENDIENTE cuando su webhook se perdio.
 *
 * <p>
 * El lote se reclama con {@link DianJobLeasePort}: este job corre en todas las
 * réplicas del backend a la vez, y sin reparto todas consultarían el estado de
 * los mismos documentos al proveedor.
 */
@Component
public class PendingReconciliationJob {
    private static final Logger log = LoggerFactory.getLogger(PendingReconciliationJob.class);
    private static final String JOB_NAME = "dian.pending.reconciliation";

    private final ElectronicDocumentRepository repository;
    private final DianJobLeasePort leasePort;
    private final DocumentTransmitter transmitter;
    private final ScheduledJobTelemetry telemetry;
    private final int batchSize;
    private final Duration lease;

    public PendingReconciliationJob(ElectronicDocumentRepository repository,
            DianJobLeasePort leasePort, DocumentTransmitter transmitter,
            ScheduledJobTelemetry telemetry,
            @Value("${dian.reconciliation.batch-size:50}") int batchSize,
            @Value("${dian.reconciliation.lease:PT15M}") Duration lease) {
        this.repository = repository;
        this.leasePort = leasePort;
        this.transmitter = transmitter;
        this.telemetry = telemetry;
        this.batchSize = batchSize;
        this.lease = lease;
    }

    @Scheduled(initialDelayString = "${dian.reconciliation.initial-delay-ms:120000}", fixedDelayString = "${dian.reconciliation.poll-delay-ms:43200000}")
    public void reconcilePending() {
        telemetry.observe(JOB_NAME, this::executeReconciliation);
    }

    private Outcome executeReconciliation() {
        List<Long> leased = leasePort.leaseByDianStatus(DianStatus.PENDIENTE, batchSize, lease);
        if (leased.isEmpty())
            return Outcome.NO_WORK;
        log.info("Reconciliando documento(s) PENDIENTE contra el proveedor DIAN: {} reclamado(s)",
                leased.size());
        int attempted = 0;
        int failures = 0;
        for (Long documentId : leased) {
            ElectronicDocument document = repository.findById(documentId).orElse(null);
            if (document == null)
                continue;
            attempted++;
            try {
                transmitter.reconcile(document);
            } catch (Exception e) {
                failures++;
                log.warn("Reconciliación falló para documento {}: {}", documentId, e.getMessage());
            }
        }
        Outcome outcome = Outcome.from(attempted, failures);
        log.info("Reconciliación DIAN finalizada: intentado(s)={}, fallido(s)={}, resultado={}",
                attempted, failures, outcome.value());
        return outcome;
    }
}
