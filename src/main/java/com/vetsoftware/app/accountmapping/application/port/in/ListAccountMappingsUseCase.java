package com.vetsoftware.app.accountmapping.application.port.in;

import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAccountMappingsUseCase {

    /**
     * Los mapeos, paginados.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas porque el puerto no transporta
     * ningun {@code companyId}</strong>, que es la señal que examina
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}. Aqui no es una concesion: la tabla
     * no tiene columna de empresa.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<AccountMappingDto> listAll(int page, int pageSize);
}
