package com.vetsoftware.app.billingdocumentstatushistory.application.port.out;

import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatusHistory;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * <strong>La unica escritura declarada es {@code save}, y solo sirve para
 * insertar.</strong> No hay {@code update}, no hay {@code delete}, no hay
 * borrado logico y no hay reactivacion: la tabla solo se agrega. Un cambio de
 * estado registrado por error no se edita ni se desactiva — se corrige moviendo
 * el documento otra vez, y las dos filas quedan. Esto es contrato, no
 * casualidad: si manana hiciera falta un {@code UPDATE}, la conversacion que
 * hay que tener antes es si esta tabla sigue siendo la bitacora irreemplazable
 * que el modelo promete.
 *
 * <p>
 * <strong>No existe ningun {@code findById(Long)} ancho, y es
 * deliberado.</strong> {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} (BE-COV) marca
 * al caso de uso que conoce la variante ancha y no la acotada; la forma de no
 * poder equivocarse es que la ancha no exista. Toda lectura por id de este
 * slice lleva la empresa.
 *
 * <p>
 * <strong>Y ningun listado acotado solo por {@code billingDocumentId}.</strong>
 * El documento pertenece a una empresa, asi que filtrar por la FK ajena no
 * cuenta como filtro de tenant — es exactamente lo que persigue
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} (BE-29). La pelicula de un documento
 * se pide siempre con las dos columnas, que ademas es el orden literal del
 * indice {@code ix_bdsh_document}.
 */
public interface BillingDocumentStatusHistoryRepository {

    BillingDocumentStatusHistory save(BillingDocumentStatusHistory entry);

    Optional<BillingDocumentStatusHistory> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * La pelicula de un documento, en orden de proyeccion.
     *
     * <p>
     * Las dos columnas van en el mismo orden que
     * {@code ix_bdsh_document (company_id, billing_document_id, occurred_at)}: el
     * indice sirve el filtro y el orden sin ordenar en memoria.
     */
    PageResult<BillingDocumentStatusHistory> findAllByCompanyIdAndBillingDocumentId(Long companyId,
            Long billingDocumentId, int page, int pageSize);

    /**
     * Los cambios de una empresa que dejaron el documento en un estado concreto.
     *
     * <p>
     * Es el material con el que se responde «cuantos documentos estaban esperando
     * factura externa a 31 de marzo»: quien pregunta filtra por
     * {@code AWAITING_EXTERNAL} y corta por {@code occurredAt}. La reconstruccion
     * completa —quedarse con la ultima transicion de cada documento anterior al
     * corte— no vive aqui todavia; ver el informe de la feature.
     */
    PageResult<BillingDocumentStatusHistory> findAllByCompanyIdAndToStatus(Long companyId,
            BillingDocumentStatus toStatus, int page, int pageSize);

    PageResult<BillingDocumentStatusHistory> findAllByCompanyId(Long companyId, int page,
            int pageSize);

    /**
     * Barrido cross-tenant de plataforma. Devuelve filas de todas las empresas, asi
     * que solo lo consume un puerto cerrado a {@code hasRole('SYSTEM')} a secas.
     */
    PageResult<BillingDocumentStatusHistory> findAll(int page, int pageSize);
}
