package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics.Channel;
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
 * de la transacción del cierre. Si esta emisión post-commit falla ANTES de que
 * la DIAN se pronuncie, el documento sigue PENDIENTE y sí es re-emitible
 * ({@code POST /{id}/transmit} o reconciliación). Si el fallo es posterior —el
 * documento ya quedó VALIDADO y lo que reventó fue el QR, el PDF o S3— NO lo
 * es: ambas rutas de recuperación filtran por PENDIENTE, así que nadie vuelve a
 * llamar a la entrega y el documento se queda validado y sin representación
 * gráfica hasta que alguien lo arregle a mano.
 */
@Component
public class ClosedAccountEmissionCompleter {
    private final ElectronicDocumentRepository repository;
    private final ElectronicDocumentEmitter emitter;
    private final DeliverElectronicDocumentService deliverService;
    private final SalesMetrics salesMetrics;

    public ClosedAccountEmissionCompleter(ElectronicDocumentRepository repository,
            ElectronicDocumentEmitter emitter, DeliverElectronicDocumentService deliverService,
            SalesMetrics salesMetrics) {
        this.repository = repository;
        this.emitter = emitter;
        this.deliverService = deliverService;
        this.salesMetrics = salesMetrics;
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
     *
     * <p>
     * El {@code companyId} viaja desde {@link EmitElectronicDocumentOnCloseService}
     * —donde ya venía en el command— para que la relectura del documento sea la
     * acotada. Esta clase no implementa ningún {@code port/in}, así que su única
     * barrera es la empresa que le pasa su llamador: sin ella, un id de documento
     * equivocado (o un llamador futuro) emitiría y entregaría el documento de otro
     * tenant.
     *
     * <p>
     * <b>La métrica de venta se publica aquí y no en
     * {@link EmitElectronicDocumentOnCloseService}</b>: allí el documento acaba de
     * nacer PENDIENTE y el resultado de la emisión todavía no se conoce, así que
     * contar la venta en ese punto contaría también las que nunca llegan a la DIAN.
     *
     * <p>
     * <b>Va ANTES de la entrega, a propósito.</b> El hecho comercial es la emisión;
     * el PDF y el correo son posteriores y su fallo no des-vende nada. Si la
     * métrica fuese detrás de {@code deliverIfValidated}, un fallo de QR/PDF/S3
     * dejaría sin contar una venta ya facturada y validada — el mismo defecto que
     * se corrigió en {@link RegisterPosSaleService}.
     */
    public void complete(Long documentId, Long companyId) {
        ElectronicDocument document = repository.findByIdAndCompanyId(documentId, companyId)
                .orElseThrow(() -> new ElectronicDocumentNotFoundException(documentId));
        ElectronicDocument emitted = emitter.emit(document);
        salesMetrics.completed(Channel.OPEN_ACCOUNT, emitted.getDocumentType(),
                emitted.getPayableAmount(), emitted.getLines().size());
        deliverService.deliverIfValidated(emitted);
    }
}
