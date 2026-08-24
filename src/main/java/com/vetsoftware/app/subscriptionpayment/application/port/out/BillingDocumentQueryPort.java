package com.vetsoftware.app.subscriptionpayment.application.port.out;

import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentRef;
import java.util.Optional;

/**
 * Resuelve el documento de cobro que vive en {@code subscriptionbilling}.
 *
 * <p>
 * <strong>Solo declara la variante acotada por empresa.</strong> Es
 * literalmente el defecto que las FK compuestas existen para impedir: con una
 * resolucion ancha, un pago de una clinica podia saldar la factura de otra
 * ({@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA}, BE-COV). La base lo
 * bloquea con {@code fk_bda_target_document}, pero el rechazo llegaria como una
 * violacion de integridad al hacer flush; acotar la lectura lo convierte en un
 * 400 con mensaje.
 */
public interface BillingDocumentQueryPort {

    Optional<BillingDocumentRef> findByIdAndCompanyId(Long documentId, Long companyId);

    /**
     * Bloqueo pesimista sobre el documento destino, acotado por empresa. Se toma
     * antes de leer las aplicaciones para serializar el recalculo de
     * {@code settled_amount} (R4).
     */
    void lockByIdAndCompanyId(Long documentId, Long companyId);
}
