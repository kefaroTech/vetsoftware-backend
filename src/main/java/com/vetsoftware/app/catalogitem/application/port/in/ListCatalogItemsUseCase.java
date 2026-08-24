package com.vetsoftware.app.catalogitem.application.port.in;

import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Listado sin filtro de empresa, y por eso {@code hasRole("SYSTEM")} a secas:
 * es exactamente lo que exige {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} (BE-29).
 * Aquí no hay un hermano acotado por empresa porque el catálogo no pertenece a
 * ninguna.
 */
public interface ListCatalogItemsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<CatalogItemDto> listAll(int page, int pageSize);
}
