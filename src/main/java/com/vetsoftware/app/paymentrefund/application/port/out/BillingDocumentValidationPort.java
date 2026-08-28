package com.vetsoftware.app.paymentrefund.application.port.out;

/**
 * La FK compuesta {@code payment_refunds (company_id, source_document_id)}
 * contra {@code subscription_billing_documents}, que es de otra feature.
 *
 * <p>
 * Es un {@code ValidationPort} y no un {@code QueryPort} porque esta feature
 * <strong>no lee ningun campo</strong> del documento: el importe que manda es
 * el del pago -que es de donde sale la plata-, no el de la factura que lo
 * origino. Traer aqui un {@code BillingDocumentRef} seria copiar datos que
 * nadie usa y atar este slice a la forma de otro. Es el caso que
 * {@code CLAUDE.md} describe como «no necesitas datos del agregado externo,
 * solo el ID».
 *
 * <p>
 * Acotado por empresa: {@code subscription_billing_documents} si pertenece a
 * una empresa, asi que la variante ancha permitiria colgar la devolucion de un
 * documento ajeno.
 */
public interface BillingDocumentValidationPort {

    /**
     * {@code true} si el documento existe y es de esa empresa. Devuelve un booleano
     * en vez de lanzar: la excepcion de FK inexistente la decide el caso de uso,
     * nunca el adaptador.
     */
    boolean existsByIdAndCompanyId(Long documentId, Long companyId);
}
