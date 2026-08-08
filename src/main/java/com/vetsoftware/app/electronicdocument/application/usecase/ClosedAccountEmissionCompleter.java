package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentNotFoundException;
import org.springframework.stereotype.Component;

/**
 * A1 — completa la emisión de un documento del cierre en su PROPIA transacción,
 * invocada tras el commit del cierre (afterCommit). Separa el I/O externo
 * (numeración local + HTTP a MATIAS + render PDF + S3 + email) del lock
 * pesimista de la cuenta: cuando esto corre, la transacción del cierre ya cerró
 * y liberó el {@code SELECT … FOR UPDATE} sobre {@code open_accounts}, así que
 * la transmisión (hasta ~60s de read-timeout) NO retiene el lock ni la conexión
 * del pool que serializa cargos/abonos concurrentes.
 *
 * <p>
 * El documento ya está persistido PENDIENTE por {@link DocumentBuilder} dentro
 * de la transacción del cierre; si esta emisión post-commit falla, queda
 * PENDIENTE y re-emitible ({@code POST
 * /{id}/transmit} o reconciliación).
 */
@Component
public class ClosedAccountEmissionCompleter {
    private final ElectronicDocumentRepository repository;
    private final ElectronicDocumentEmitter emitter;
    private final DeliverElectronicDocumentService deliverService;

    public ClosedAccountEmissionCompleter(ElectronicDocumentRepository repository,
            ElectronicDocumentEmitter emitter, DeliverElectronicDocumentService deliverService) {
        this.repository = repository;
        this.emitter = emitter;
        this.deliverService = deliverService;
    }

    /**
     * Sin {@code @Transactional}, igual que los otros cinco casos de uso que llaman
     * a {@link ElectronicDocumentEmitter}. Tenerlo anulaba media A1: sacar la
     * emisión del commit del cierre liberaba el lock de {@code open_accounts}, pero
     * abría acto seguido otra transacción que retenía una conexión del pool —y el
     * {@code FOR UPDATE} del consecutivo— durante los hasta 75 segundos del HTTP a
     * MATIAS. El lock de facturación solo se había mudado de tabla.
     *
     * <p>
     * Cada paso abre ahora la suya, corta: la numeración en {@link NumberAssigner}
     * ({@code REQUIRES_NEW}) y el desenlace en {@link TransmissionResultPersister}.
     * La lectura inicial no necesita ninguna.
     */
    public void complete(Long documentId) {
        ElectronicDocument document = repository.findById(documentId)
                .orElseThrow(() -> new ElectronicDocumentNotFoundException(documentId));
        ElectronicDocument emitted = emitter.emit(document);
        deliverService.deliverIfValidated(emitted);
    }
}
