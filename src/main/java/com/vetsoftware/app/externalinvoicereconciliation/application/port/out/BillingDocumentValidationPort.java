package com.vetsoftware.app.externalinvoicereconciliation.application.port.out;

/**
 * La FK compuesta {@code (company_id, billing_document_id)} contra
 * {@code subscription_billing_documents}, que es de otra feature.
 *
 * <p>
 * Es un {@code ValidationPort} y no un {@code QueryPort} porque esta feature
 * <strong>no lee ningun campo</strong> del documento. Podria parecer que si -el
 * total propio esta ahi-, y esa es justamente la trampa: si el
 * {@code computedTotal} se copiara del documento en el momento de conciliar, la
 * conciliacion dejaria de ser una comparacion entre dos hechos independientes y
 * pasaria a compararse consigo misma el dia que el documento se corrija. El
 * total propio lo trae quien abre la conciliacion y queda congelado aqui.
 *
 * <p>
 * Acotado por empresa: {@code subscription_billing_documents} pertenece a una
 * empresa, y la FK de la base es compuesta, asi que una variante ancha
 * permitiria abrir la conciliacion contra un documento ajeno y el error
 * llegaria como una violacion de integridad sin explicacion.
 */
public interface BillingDocumentValidationPort {

    /**
     * {@code true} si el documento existe y es de esa empresa. Devuelve un booleano
     * en vez de lanzar: la excepcion de FK inexistente la decide el caso de uso,
     * nunca el adaptador.
     */
    boolean existsByIdAndCompanyId(Long documentId, Long companyId);
}
