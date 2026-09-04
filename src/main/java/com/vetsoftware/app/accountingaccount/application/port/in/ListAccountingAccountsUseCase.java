package com.vetsoftware.app.accountingaccount.application.port.in;

import com.vetsoftware.app.accountingaccount.application.dto.AccountingAccountDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAccountingAccountsUseCase {

    /**
     * El plan de cuentas completo, paginado.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas porque el puerto no transporta
     * ningun {@code companyId}</strong>, que es exactamente la señal que examina
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} (regla dura, BE-29). Aqui no es una
     * concesion: la tabla no tiene columna de empresa y la lista es la de los
     * libros de Lumbre. Anadir un {@code companyId} solo para abrirla por permiso
     * seria fingir un filtro que no existe.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<AccountingAccountDto> listAll(int page, int pageSize);
}
