package com.vetsoftware.app.electronicdocument.infrastructure.scheduling;

import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics.Origin;
import com.vetsoftware.app.electronicdocument.application.port.out.DianJobLeasePort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.application.usecase.DocumentTransmitter;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reintenta los documentos que quedaron en CONTINGENCIA (proveedor/DIAN
 * indisponible al emitir). Cada documento se retransmite en su propia
 * transacción ({@link DocumentTransmitter}); un fallo no afecta a los demás. Si
 * el reintento resuelve, el documento pasa a VALIDADO/RECHAZADO; si no, sigue
 * en CONTINGENCIA y se reintenta en el siguiente ciclo.
 *
 * <p>
 * <b>El ciclo es de 12h</b> ({@code dian.contingency.retry-delay-ms}), contadas
 * desde que TERMINA la pasada anterior; no es una hora fija del dia. El reloj
 * se reinicia en cada despliegue, y el {@code initial-delay-ms} de 60s es la
 * unica pasada rapida que queda: un documento que cae en contingencia justo
 * despues de una corrida espera hasta 12h a su primer reintento automatico.
 *
 * <p>
 * El reintento se acota por dos límites (configurables): un <b>cap de
 * intentos</b> ({@code
 * dian.contingency.max-attempts}) y una <b>ventana de plazo</b> desde la
 * creación ({@code
 * dian.contingency.deadline-hours}). Agotado cualquiera de los dos, el
 * documento se deja en CONTINGENCIA pero deja de reintentarse automáticamente
 * (se registra un error para atención manual: reemisión vía
 * {@code POST /electronic-documents/{id}/transmit} o corrección por nota).
 *
 * <p>
 * El lote se reclama con {@link DianJobLeasePort}. Este job corre en todas las
 * réplicas del backend a la vez: sin reparto, N réplicas retransmitirían el
 * mismo documento a la DIAN en el mismo ciclo, y con un proveedor que no
 * deduplique eso son documentos fiscales repetidos.
 */
@Component
public class ContingencyRetryJob {
    private static final Logger log = LoggerFactory.getLogger(ContingencyRetryJob.class);
    private static final String JOB_NAME = "dian.contingency.retry";

    private final ElectronicDocumentRepository repository;
    private final DianJobLeasePort leasePort;
    private final DocumentTransmitter transmitter;
    private final TransmissionLogPort transmissionLog;
    private final ScheduledJobTelemetry telemetry;
    private final int maxAttempts;
    private final long deadlineHours;
    private final int batchSize;
    private final Duration lease;

    public ContingencyRetryJob(ElectronicDocumentRepository repository, DianJobLeasePort leasePort,
            DocumentTransmitter transmitter, TransmissionLogPort transmissionLog,
            ScheduledJobTelemetry telemetry,
            @Value("${dian.contingency.max-attempts:4}") int maxAttempts,
            @Value("${dian.contingency.deadline-hours:48}") long deadlineHours,
            @Value("${dian.contingency.batch-size:25}") int batchSize,
            @Value("${dian.contingency.lease:PT30M}") Duration lease) {
        this.repository = repository;
        this.leasePort = leasePort;
        this.transmitter = transmitter;
        this.transmissionLog = transmissionLog;
        this.telemetry = telemetry;
        this.maxAttempts = maxAttempts;
        this.deadlineHours = deadlineHours;
        this.batchSize = batchSize;
        this.lease = lease;
    }

    @Scheduled(initialDelayString = "${dian.contingency.initial-delay-ms:60000}", fixedDelayString = "${dian.contingency.retry-delay-ms:43200000}")
    public void retryContingencies() {
        telemetry.observe(JOB_NAME, this::executeRetries);
    }

    private Outcome executeRetries() {
        List<Long> leased = leasePort.leaseByDianStatus(DianStatus.CONTINGENCIA, batchSize, lease);
        if (leased.isEmpty())
            return Outcome.NO_WORK;
        LocalDateTime deadlineThreshold = LocalDateTime.now().minusHours(deadlineHours);
        log.info("Reintentando documento(s) en contingencia DIAN: {} reclamado(s)", leased.size());
        int attempted = 0;
        int failures = 0;
        for (Long documentId : leased) {
            ElectronicDocument document = repository.findById(documentId).orElse(null);
            if (document == null || isExhausted(document, deadlineThreshold))
                continue;
            attempted++;
            try {
                transmitter.transmit(document, Origin.RETRY);
            } catch (Exception e) {
                failures++;
                log.warn("Reintento de contingencia falló para documento {}: {}", documentId,
                        e.getMessage());
            }
        }
        Outcome outcome = Outcome.from(attempted, failures);
        log.info(
                "Reintento de contingencias DIAN finalizado: intentado(s)={}, fallido(s)={}, resultado={}",
                attempted, failures, outcome.value());
        return outcome;
    }

    /**
     * Un documento ya no se reintenta si superó el cap de intentos o la ventana de
     * plazo.
     */
    private boolean isExhausted(ElectronicDocument document, LocalDateTime deadlineThreshold) {
        int attempts = transmissionLog.countAttempts(document.getId());
        if (attempts >= maxAttempts) {
            log.error(
                    "Documento {} agotó el cap de {} reintentos de contingencia DIAN; "
                            + "requiere atención manual (reemitir o corregir por nota).",
                    document.getId(), maxAttempts);
            return true;
        }
        if (document.getCreatedDate() != null
                && document.getCreatedDate().isBefore(deadlineThreshold)) {
            log.error(
                    "Documento {} superó la ventana de {}h del plazo de contingencia DIAN; "
                            + "requiere atención manual (reemitir o corregir por nota).",
                    document.getId(), deadlineHours);
            return true;
        }
        return false;
    }
}
