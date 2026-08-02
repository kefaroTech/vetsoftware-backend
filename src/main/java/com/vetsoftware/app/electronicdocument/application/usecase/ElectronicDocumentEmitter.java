package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.BillingEntitlementQueryPort;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import org.springframework.stereotype.Component;

/**
 * Punto único de emisión: decide, según el derecho de la empresa (submódulo
 * BILLING), entre - numerar fiscalmente + transmitir al proveedor DIAN (empresa
 * habilitada), o - dejar el documento PENDIENTE (empresa sin el módulo): datos
 * guardados, sin consumir consecutivo y sin MATIAS, pero re-emitible más
 * adelante al habilitar el módulo (la reconciliación lo ignora porque no tiene
 * clave de proveedor: nunca se transmitió).
 *
 * <p>
 * Asume que el documento ya está persistido (PENDIENTE, sin número):
 * {@link DocumentBuilder} / {@link PosSaleDocumentBuilder} lo guardan al
 * construirlo y los casos de uso de notas lo guardan antes.
 */
@Component
public class ElectronicDocumentEmitter {
    private final BillingEntitlementQueryPort billingEntitlement;
    private final NumberAssigner numberAssigner;
    private final DocumentTransmitter documentTransmitter;

    public ElectronicDocumentEmitter(BillingEntitlementQueryPort billingEntitlement,
            NumberAssigner numberAssigner, DocumentTransmitter documentTransmitter) {
        this.billingEntitlement = billingEntitlement;
        this.numberAssigner = numberAssigner;
        this.documentTransmitter = documentTransmitter;
    }

    public ElectronicDocument emit(ElectronicDocument document) {
        // Sin BILLING: NO se numera ni se contacta a MATIAS. El documento queda
        // PENDIENTE (ya
        // persistido)
        // para que, al habilitar el módulo, pueda emitirse el backlog (POST
        // /{id}/transmit).
        if (!billingEntitlement.isElectronicInvoicingEnabled(document.getCompanyId())) {
            return document;
        }
        // POS: el consecutivo lo asigna el proveedor (MATIAS auto-increment), así que
        // NO consumimos
        // numeración
        // local — solo aportamos resolución+prefijo (requeridos por el endpoint). El
        // resto numera
        // localmente.
        if (document.usesProviderAssignedConsecutive()) {
            numberAssigner.assignResolutionOnly(document);
        } else {
            numberAssigner.assign(document);
        }
        return documentTransmitter.transmit(document);
    }
}
