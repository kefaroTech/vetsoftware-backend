package com.vetsoftware.app.billingdocumentstatushistory.application.port.out;

/**
 * La FK compuesta
 * {@code billing_document_status_history (company_id, billing_document_id)}
 * contra {@code subscription_billing_documents (company_id, id)}, que es de
 * otra feature.
 *
 * <p>
 * Es un {@code ValidationPort} y no un {@code QueryPort} porque esta feature
 * <strong>no lee ningun campo</strong> del documento. Podria parecer que si
 * —«comprueba que el {@code fromStatus} coincide con el {@code issue_status}
 * actual»— y traerse ese dato seria justo el error: la bitacora apunta lo que
 * ocurrio, y para eso tendria que resolver una carrera contra la escritura que
 * esta moviendo el documento en ese mismo instante. La coherencia entre el
 * documento y su pelicula la decide {@code subscriptionbilling}; aqui solo se
 * exige que el documento exista y sea de esta empresa. Es el caso que
 * {@code CLAUDE.md} describe como «no necesitas datos del agregado externo,
 * solo el ID».
 *
 * <p>
 * <strong>Acotado por empresa, sin variante ancha.</strong> Con
 * {@code existsById(id)} a secas, una clinica podria colgar un fotograma de la
 * factura de la vecina y meterle ruido en la pelicula de un documento que no es
 * suyo — y el error solo se veria mas tarde, como un choque de la FK compuesta
 * disfrazado de 500.
 */
public interface BillingDocumentValidationPort {

    /**
     * {@code true} si el documento existe y es de esa empresa. Devuelve un booleano
     * en vez de lanzar: la excepcion de FK inexistente la decide el caso de uso,
     * nunca el adaptador.
     */
    boolean existsByIdAndCompanyId(Long billingDocumentId, Long companyId);
}
