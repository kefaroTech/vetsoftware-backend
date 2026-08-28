package com.vetsoftware.app.externalinvoicereconciliation.application.port.out;

import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliation;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * <strong>La carga por id es ancha a proposito, y aqui si es lo
 * correcto.</strong> {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} (BE-COV) exime
 * expresamente al servicio que solo alcanza SYSTEM: un principal SYSTEM no
 * tiene empresa propia contra la que acotar. Toda la rodaja esta cerrada a
 * {@code hasRole('SYSTEM')} a secas, asi que declarar una variante
 * {@code findByIdAndCompanyId} solo simularia una comprobacion de propiedad que
 * ningun llamante de este camino puede aportar.
 *
 * <p>
 * <strong>Ninguna escritura salvo {@code save}</strong>: una conciliacion no se
 * desactiva ni se borra, se resuelve. Por eso la tabla tampoco lleva
 * {@code enabled}.
 */
public interface ExternalInvoiceReconciliationRepository {

    ExternalInvoiceReconciliation save(ExternalInvoiceReconciliation reconciliation);

    Optional<ExternalInvoiceReconciliation> findById(Long id);

    /**
     * Espejo de {@code uq_eir_document}. Se consulta <strong>antes</strong> de
     * insertar: la unicidad la cuida la base de verdad, pero el segundo intento
     * tiene que salir como un 409 que explica que ya existe y no como el 500 sin
     * explicacion en que se convierte una violacion de indice unico.
     */
    boolean existsByCompanyIdAndBillingDocumentId(Long companyId, Long billingDocumentId);

    /** Barrido de plataforma cross-tenant. Solo lo consume un puerto SYSTEM. */
    PageResult<ExternalInvoiceReconciliation> findAll(int page, int pageSize);

    PageResult<ExternalInvoiceReconciliation> findAllByCompanyId(Long companyId, int page,
            int pageSize);

    /**
     * La bandeja por estado. Su unico consumidor real es la de
     * {@code MISSING_EXTERNAL}, y su orden es por antiguedad para servir
     * {@code ix_eir_pending (status, created_date)}.
     */
    PageResult<ExternalInvoiceReconciliation> findAllByStatus(
            ExternalInvoiceReconciliationStatus status, int page, int pageSize);
}
