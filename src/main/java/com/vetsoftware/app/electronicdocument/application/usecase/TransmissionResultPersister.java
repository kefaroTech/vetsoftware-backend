package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.ContingencyMonitorPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderResult;
import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.TransmissionResult;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste el desenlace de una transmisión a la DIAN en una transacción propia
 * y CORTA, que se abre DESPUÉS de que el HTTP ya terminó.
 *
 * <p>
 * Vive en su propio bean y no como método privado de
 * {@link DocumentTransmitter} por una razón mecánica: Spring aplica
 * {@code @Transactional} mediante un proxy, y una llamada interna entre métodos
 * de la misma clase no pasa por él. Un {@code persistResult} privado dentro del
 * transmisor se ejecutaría con la transacción del caller —o sin ninguna— y el
 * arreglo sería silenciosamente inútil.
 *
 * <p>
 * {@code REQUIRES_NEW} porque el desenlace debe quedar guardado aunque el
 * caller tenga una transacción viva por otro motivo: lo que no puede pasar es
 * que el resultado de la DIAN dependa de una transacción que abrió antes del
 * HTTP.
 */
@Component
public class TransmissionResultPersister {
    private static final Logger log = LoggerFactory.getLogger(TransmissionResultPersister.class);

    private final ElectronicDocumentRepository repository;
    private final TransmissionLogPort transmissionLog;
    private final CreditNoteReversalApplier reversalApplier;
    private final NumberAssigner numberAssigner;
    private final ContingencyMonitorPort contingencyMonitor;

    public TransmissionResultPersister(ElectronicDocumentRepository repository,
            TransmissionLogPort transmissionLog, CreditNoteReversalApplier reversalApplier,
            NumberAssigner numberAssigner, ContingencyMonitorPort contingencyMonitor) {
        this.repository = repository;
        this.transmissionLog = transmissionLog;
        this.reversalApplier = reversalApplier;
        this.numberAssigner = numberAssigner;
        this.contingencyMonitor = contingencyMonitor;
    }

    /**
     * Aplica el resultado del proveedor al documento, lo persiste, deja bitácora y
     * ejecuta el reverso de cartera si la transmisión dejó validada una nota
     * crédito.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ElectronicDocument persist(ElectronicDocument document, ProviderResult result,
            String providerName) {
        // Deteccion automatica del modo contingencia: CONTINGENCIA = fallo de
        // infraestructura (5xx/timeout); cualquier otro estado = el proveedor respondio
        // (sano).
        contingencyMonitor.recordOutcome(document.getCompanyId(),
                result.status() != DianStatus.CONTINGENCIA);
        applyResult(document, result);
        ElectronicDocument saved = repository.updateDianResult(document);

        transmissionLog.record(document.getId(), providerName, result.httpStatus(),
                result.providerDocumentKey(), toTransmissionResult(result.status()),
                result.rejectionReason());

        // Subordinacion del void: si esta transmision dejo VALIDADA una nota credito
        // (proveedor sincrono), reversa la factura referenciada y la cartera en el
        // acto.
        reversalApplier.applyIfCreditNoteValidated(saved);
        return saved;
    }

    /**
     * Variante para la reconciliación: el proveedor ya tenía una clave de
     * transmisión previa y no se vuelve a evaluar el modo contingencia.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ElectronicDocument persistReconciled(ElectronicDocument document, ProviderResult result,
            String providerName, String providerKey) {
        applyResult(document, result);
        ElectronicDocument saved = repository.updateDianResult(document);
        transmissionLog.record(document.getId(), providerName, result.httpStatus(), providerKey,
                toTransmissionResult(result.status()), result.rejectionReason());
        reversalApplier.applyIfCreditNoteValidated(saved);
        return saved;
    }

    private void applyResult(ElectronicDocument document, ProviderResult r) {
        switch (r.status()) {
            case VALIDADO -> {
                // Alerta de seguridad: un documento NUNCA deberia quedar VALIDADO sin sello
                // fiscal (CUFE en factura / CUDE en POS y notas). El proveedor ya degrada a
                // PENDIENTE el "00 sin sello"; esto es la red por si otra ruta lo marcara
                // validado sin CUFE/CUDE.
                if (isBlank(r.cufe()) && isBlank(r.cude())) {
                    log.error("Documento {} marcado VALIDADO SIN SELLO (CUFE/CUDE vacíos). "
                            + "Revisar la respuesta del proveedor; requiere atención manual.",
                            document.getId());
                }
                document.markValidated(r.prefix(), r.consecutive(), r.cufe(), r.cude(), r.uuid(),
                        r.xmlSigned(), r.qrData(), r.qrUrl(), r.pdfRepresentation(),
                        r.validationDate() != null ? r.validationDate() : LocalDateTime.now());
            }
            case RECHAZADO -> {
                document.markRejected();
                // Recupera el consecutivo (si es seguro) para no dejar un hueco en la secuencia
                // fiscal. El persist posterior (updateDianResult) guarda la numeracion limpia.
                numberAssigner.release(document);
            }
            case CONTINGENCIA -> document.markContingency();
            case PENDIENTE -> {
                /* async: el webhook completara el estado */
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static TransmissionResult toTransmissionResult(DianStatus status) {
        return switch (status) {
            case VALIDADO -> TransmissionResult.ACCEPTED;
            case RECHAZADO -> TransmissionResult.REJECTED;
            case CONTINGENCIA -> TransmissionResult.ERROR;
            case PENDIENTE -> TransmissionResult.PENDING;
            case NO_ELECTRONICO ->
                throw new IllegalStateException("NO_ELECTRONICO no es un resultado de transmisión");
        };
    }
}
