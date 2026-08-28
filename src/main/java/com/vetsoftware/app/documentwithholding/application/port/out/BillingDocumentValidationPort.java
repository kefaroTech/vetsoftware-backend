package com.vetsoftware.app.documentwithholding.application.port.out;

/**
 * La FK compuesta
 * {@code document_withholdings (company_id, billing_document_id)} contra
 * {@code subscription_billing_documents}, que es de otra feature.
 *
 * <p>
 * Es un {@code ValidationPort} y no un {@code QueryPort} porque esta feature
 * <strong>no lee ningun campo</strong> del documento. Podria parecer que si
 * —«la base gravable no deberia pasarse del total de la factura»— y es
 * precisamente lo contrario: la retencion se calcula sobre la base gravable que
 * el cliente decidio, que puede no coincidir con el total del documento (el IVA
 * no forma parte de la base de retefuente, y la de ICA depende del municipio).
 * Traer aqui un {@code BillingDocumentRef} invitaria a inventarse esa
 * comprobacion y a rechazar retenciones legitimas. Es el caso que
 * {@code CLAUDE.md} describe como «no necesitas datos del agregado externo,
 * solo el ID».
 *
 * <p>
 * Acotado por empresa: el documento pertenece a una, asi que la variante ancha
 * permitiria colgar la retencion de una factura ajena — y ese es justo el
 * escenario que la FK compuesta existe para impedir.
 */
public interface BillingDocumentValidationPort {

    /**
     * {@code true} si el documento existe y es de esa empresa. Devuelve un booleano
     * en vez de lanzar: la excepcion de FK inexistente la decide el caso de uso,
     * nunca el adaptador.
     */
    boolean existsByIdAndCompanyId(Long billingDocumentId, Long companyId);
}
