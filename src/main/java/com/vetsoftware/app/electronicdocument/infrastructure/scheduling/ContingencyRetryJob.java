package com.vetsoftware.app.electronicdocument.infrastructure.scheduling;

import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.usecase.DocumentTransmitter;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reintenta los documentos que quedaron en CONTINGENCIA (proveedor/DIAN indisponible al emitir).
 * Cada documento se retransmite en su propia transacción ({@link DocumentTransmitter}); un fallo no
 * afecta a los demás. Si el reintento resuelve, el documento pasa a VALIDADO/RECHAZADO; si no, sigue
 * en CONTINGENCIA y se reintenta en el siguiente ciclo.
 *
 * TODO: cap de intentos / ventana de plazo legal, y reconciliación de PENDIENTE por polling
 * (webhooks perdidos) — ver F3_DIAN_MATIAS_INTEGRATION_PLAN.md.
 */
@Component
public class ContingencyRetryJob {
    private static final Logger log = LoggerFactory.getLogger(ContingencyRetryJob.class);

    private final ElectronicDocumentRepository repository;
    private final DocumentTransmitter transmitter;

    public ContingencyRetryJob(ElectronicDocumentRepository repository, DocumentTransmitter transmitter) {
        this.repository = repository;
        this.transmitter = transmitter;
    }

    @Scheduled(
            initialDelayString = "${dian.contingency.initial-delay-ms:60000}",
            fixedDelayString = "${dian.contingency.retry-delay-ms:300000}")
    public void retryContingencies() {
        List<ElectronicDocument> pending = repository.findByDianStatus(DianStatus.CONTINGENCIA);
        if (pending.isEmpty()) return;
        log.info("Reintentando {} documento(s) en contingencia DIAN", pending.size());
        for (ElectronicDocument document : pending) {
            try {
                transmitter.transmit(document);
            } catch (Exception e) {
                log.warn("Reintento de contingencia falló para documento {}: {}",
                        document.getId(), e.getMessage());
            }
        }
    }
}
