package com.vetsoftware.app.catalogitemlimit.application.port.in;

import com.vetsoftware.app.catalogitemlimit.application.command.CreateCatalogItemLimitCommand;
import com.vetsoftware.app.catalogitemlimit.application.dto.CatalogItemLimitDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Declara el techo de fábrica de un artículo.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas. Es catálogo global: la tabla
 * no tiene {@code company_id} y no hay empresa que revalidar.
 */
public interface CreateCatalogItemLimitUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CatalogItemLimitDto execute(CreateCatalogItemLimitCommand command);
}
