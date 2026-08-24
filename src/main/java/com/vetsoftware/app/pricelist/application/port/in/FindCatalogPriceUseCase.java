package com.vetsoftware.app.pricelist.application.port.in;

import com.vetsoftware.app.pricelist.application.dto.CatalogPriceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindCatalogPriceUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CatalogPriceDto findById(Long id);
}
