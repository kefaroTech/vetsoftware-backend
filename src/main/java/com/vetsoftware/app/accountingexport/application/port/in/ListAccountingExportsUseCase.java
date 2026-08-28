package com.vetsoftware.app.accountingexport.application.port.in;

import com.vetsoftware.app.accountingexport.application.dto.AccountingExportDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAccountingExportsUseCase {

    /**
     * La bandeja completa, mas reciente primero.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas porque el puerto no transporta
     * ningun {@code companyId}</strong>, que es la señal que examina
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}. Aqui no hay empresa que
     * transportar: la tabla no tiene la columna.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<AccountingExportDto> listAll(int page, int pageSize);

    /** La bandeja de un mes concreto. */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<AccountingExportDto> listByPeriod(String periodKey, int page, int pageSize);
}
