package com.vetsoftware.app.externalinvoicingoutage.application.port.out;

import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import java.util.Optional;

/**
 * <strong>Ningun metodo de este puerto recibe {@code companyId}, y esa ausencia
 * es una afirmacion sobre el modelo, no un olvido.</strong>
 * {@code external_invoicing_outages} no tiene columna de empresa: la caida es
 * un hecho de la plataforma que alcanza a varias clinicas a la vez. Anadir aqui
 * un {@code findAllByCompanyId} exigiria inventarse la columna.
 *
 * <p>
 * Esa misma ausencia es la que deja fuera de alcance a las reglas de tenancy de
 * BE-COV y BE-29, que se activan sobre el repositorio que <em>si</em> sabe
 * filtrar por empresa. Lo que reparte por clinica es
 * {@link ExternalInvoicingOutageCompanyRepository}, y por eso su listado esta
 * cerrado a plataforma en el puerto de entrada.
 */
public interface ExternalInvoicingOutageRepository {

    ExternalInvoicingOutage save(ExternalInvoicingOutage outage);

    Optional<ExternalInvoicingOutage> findById(Long id);

    /** El historico completo, paginado. No hay hermano acotado: no hay empresa. */
    PageResult<ExternalInvoicingOutage> findAll(int page, int pageSize);

    /**
     * <strong>Las que siguen vivas.</strong> Se apoya en
     * {@code ix_eio_open (ended_at, started_at)}, que sirve exactamente a este
     * {@code WHERE ended_at IS NULL ORDER BY started_at}: MySQL indexa los nulos y
     * los busca por indice —lo que no existe en este motor son indices parciales—.
     *
     * <p>
     * Devuelve {@code List} sin paginar porque {@code uq_eio_open} acota el
     * resultado a una fila por causante, y los causantes son cuatro.
     */
    List<ExternalInvoicingOutage> findAllOpen();
}
