package com.vetsoftware.app.pricelist.application.port.in;

import com.vetsoftware.app.pricelist.application.dto.CatalogPriceDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Acotar por {@code priceListId} <strong>no</strong> es filtrar por empresa: la
 * lista de precios es global. Mismo criterio que en BE-29 con las FK ajenas,
 * asi que el gate sigue siendo {@code hasRole('SYSTEM')} a secas.
 */
public interface ListCatalogPricesUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<CatalogPriceDto> listByPriceList(Long priceListId, int page, int pageSize);
}
