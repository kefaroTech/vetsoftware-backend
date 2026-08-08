package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.TransmissionResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica el rechazo que reporta un webhook: marca el documento, recupera el
 * consecutivo y deja la bitácora, todo en una transacción propia y corta.
 *
 * <p>
 * Vive en su propio bean —y no como un método privado de
 * {@link ProcessProviderWebhookService}— porque {@code @Transactional} se
 * aplica por proxy: una llamada interna entre métodos de la misma clase no
 * abriría transacción ninguna y las tres escrituras quedarían sueltas, cada una
 * en su auto-commit. Es la misma razón por la que existe
 * {@link TransmissionResultPersister}.
 */
@Component
public class WebhookRejectionApplier {

    private final ElectronicDocumentRepository repository;
    private final TransmissionLogPort transmissionLog;
    private final NumberAssigner numberAssigner;

    public WebhookRejectionApplier(ElectronicDocumentRepository repository,
            TransmissionLogPort transmissionLog, NumberAssigner numberAssigner) {
        this.repository = repository;
        this.transmissionLog = transmissionLog;
        this.numberAssigner = numberAssigner;
    }

    /**
     * @param document
     *            documento a marcar como rechazado
     * @param provider
     *            nombre del proveedor, para la bitácora
     * @param providerDocumentKey
     *            clave del documento en el proveedor
     * @param rejectionReason
     *            motivo que reportó el proveedor
     */
    @Transactional
    public void apply(ElectronicDocument document, String provider, String providerDocumentKey,
            String rejectionReason) {
        document.markRejected();
        // Recupera el consecutivo (si es seguro) para no dejar un hueco en la secuencia
        // fiscal antes de persistir la numeración limpia.
        numberAssigner.release(document);
        repository.updateDianResult(document);
        transmissionLog.record(document.getId(), provider, 200, providerDocumentKey,
                TransmissionResult.REJECTED, rejectionReason);
    }
}
