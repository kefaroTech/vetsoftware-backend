package com.vetsoftware.app.billingdocumentstatushistory.infrastructure.persistence;

import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Sin ninguna {@code @Query} de {@code UPDATE} ni de {@code DELETE}, y
 * no es que aun no hayan hecho falta.</strong> La tabla solo se agrega: no
 * existe ninguna mutacion de fila en toda la feature, ni por entidad gestionada
 * ni masiva. Eso deja fuera de un plumazo las dos trampas que {@code CLAUDE.md}
 * documenta —el {@code UPDATE} masivo que no mueve la {@code version} y el
 * {@code @SQLDelete} que no la respeta— porque aqui no hay version que mover ni
 * borrado que acotar.
 *
 * <p>
 * <strong>Todas las consultas van derivadas y todas empiezan por
 * {@code CompanyId}.</strong> No hay ni un {@code findAllByBillingDocumentId}
 * suelto: el documento pertenece a una empresa, asi que acotar por esa FK no
 * cuenta como filtro de tenant ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}). El
 * unico listado sin empresa es el {@code findAll(Pageable)} heredado de
 * {@code JpaRepository}, que solo alcanza un caso de uso cerrado a
 * {@code hasRole('SYSTEM')} a secas.
 */
public interface BillingDocumentStatusHistoryJpaRepository
        extends
            JpaRepository<BillingDocumentStatusHistoryJpaEntity, Long> {

    Optional<BillingDocumentStatusHistoryJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * La pelicula de un documento. El par de columnas es el prefijo literal de
     * {@code ix_bdsh_document (company_id, billing_document_id, occurred_at)}, asi
     * que el indice sirve el filtro y ademas el orden.
     */
    Page<BillingDocumentStatusHistoryJpaEntity> findAllByCompanyIdAndBillingDocumentId(
            Long companyId, Long billingDocumentId, Pageable pageable);

    /**
     * La bandeja de vigilancia por estado de destino.
     *
     * <p>
     * <strong>{@code ix_bdsh_document} no la sirve</strong> —no lleva
     * {@code to_status} y su segunda columna es el documento—, asi que esta
     * consulta recorre por {@code company_id} y filtra. Es aceptable mientras la
     * historia de una empresa quepa en el orden de magnitud de sus documentos; si
     * deja de serlo, el arreglo es un indice
     * {@code (company_id, to_status, occurred_at)} y no una consulta mas lista.
     */
    Page<BillingDocumentStatusHistoryJpaEntity> findAllByCompanyIdAndToStatus(Long companyId,
            BillingDocumentStatus toStatus, Pageable pageable);

    Page<BillingDocumentStatusHistoryJpaEntity> findAllByCompanyId(Long companyId,
            Pageable pageable);
}
