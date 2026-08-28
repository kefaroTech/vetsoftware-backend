package com.vetsoftware.app.catalogitemlimit.application.port.in;

import com.vetsoftware.app.catalogitemlimit.application.dto.CatalogItemLimitDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Los techos de fábrica de un artículo.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas. El filtro es por artículo,
 * no por empresa, y acotar por una clave ajena no cuenta como filtro de tenant
 * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}).
 */
public interface ListCatalogItemLimitsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    List<CatalogItemLimitDto> listByCatalogItemId(Long catalogItemId);
}
