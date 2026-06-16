package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.BillingEntitlementQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import org.springframework.stereotype.Component;

/**
 * Punto único de emisión: decide, según el derecho de la empresa (submódulo BILLING), entre
 *  - numerar fiscalmente + transmitir al proveedor DIAN (empresa habilitada), o
 *  - guardar el documento localmente (NO_ELECTRONICO): sin consumir consecutivo, sin MATIAS y excluido
 *    de la reconciliación.
 *
 * Asume que el documento ya está persistido (PENDIENTE, sin número): {@link DocumentBuilder} lo guarda al
 * construirlo y los casos de uso de notas lo guardan antes de invocar este emisor.
 */
@Component
public class ElectronicDocumentEmitter {
    private final BillingEntitlementQueryPort billingEntitlement;
    private final NumberAssigner numberAssigner;
    private final DocumentTransmitter documentTransmitter;
    private final ElectronicDocumentRepository repository;

    public ElectronicDocumentEmitter(BillingEntitlementQueryPort billingEntitlement,
                                     NumberAssigner numberAssigner,
                                     DocumentTransmitter documentTransmitter,
                                     ElectronicDocumentRepository repository) {
        this.billingEntitlement = billingEntitlement;
        this.numberAssigner = numberAssigner;
        this.documentTransmitter = documentTransmitter;
        this.repository = repository;
    }

    public ElectronicDocument emit(ElectronicDocument document) {
        if (!billingEntitlement.isElectronicInvoicingEnabled(document.getCompanyId())) {
            document.markLocal();
            return repository.updateDianResult(document); // guardado local: sin número, sin MATIAS
        }
        numberAssigner.assign(document);
        return documentTransmitter.transmit(document);
    }
}
