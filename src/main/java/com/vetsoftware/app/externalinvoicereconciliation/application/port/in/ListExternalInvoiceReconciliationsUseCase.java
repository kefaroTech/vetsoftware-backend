package com.vetsoftware.app.externalinvoicereconciliation.application.port.in;

import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El barrido de conciliaciones de la consola de plataforma.
 */
public interface ListExternalInvoiceReconciliationsUseCase {

    /**
     * <strong>{@code hasRole('SYSTEM')} a secas y sin alternativa por
     * permiso.</strong> Con {@code companyId} vacio devuelve filas de todas las
     * empresas, que es exactamente el listado que
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} (BE-29) persigue; y con
     * {@code companyId} tampoco se abre al tenant, porque este bloque no tiene
     * camino de tenant en absoluto -ver
     * {@link FindExternalInvoiceReconciliationUseCase#findById(Long)}-.
     *
     * @param companyId
     *            filtro opcional de la consola. Cuando viene, acota; cuando no, el
     *            barrido es completo
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<ExternalInvoiceReconciliationDto> listAll(Long companyId, int page, int pageSize);
}
