package com.vetsoftware.app.externalinvoicereconciliation.infrastructure.persistence;

import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Sin ninguna {@code @Query} de {@code UPDATE} ni de
 * {@code DELETE}</strong>, y no es que aun no hayan hecho falta: la fila se
 * modifica siempre por el ciclo leer-modificar-guardar de la entidad, que es el
 * unico camino donde el {@code @Version} protege de verdad. Una escritura
 * masiva por {@code @Query} lo esquivaria
 * -{@code UPDATE_MASIVO_MUEVE_LA_VERSION}, #53- y ademas se saltaria la
 * clasificacion del dominio, dejando un {@code status} que no corresponde a su
 * {@code difference}.
 *
 * <p>
 * Los tres metodos derivados sirven los tres indices declarados en el
 * changeset: {@code ix_eir_pending (status, created_date)} para la bandeja de
 * lo que nadie facturo, y el filtro por empresa para el barrido acotado de la
 * consola.
 */
public interface ExternalInvoiceReconciliationJpaRepository
        extends
            JpaRepository<ExternalInvoiceReconciliationJpaEntity, Long> {

    boolean existsByCompanyIdAndBillingDocumentId(Long companyId, Long billingDocumentId);

    Page<ExternalInvoiceReconciliationJpaEntity> findAllByCompanyId(Long companyId,
            Pageable pageable);

    Page<ExternalInvoiceReconciliationJpaEntity> findAllByStatus(
            ExternalInvoiceReconciliationStatus status, Pageable pageable);
}
