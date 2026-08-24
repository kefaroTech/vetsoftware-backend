package com.vetsoftware.app.pricelist.application.port.in;

import com.vetsoftware.app.pricelist.application.command.CreateCatalogPriceCommand;
import com.vetsoftware.app.pricelist.application.dto.CatalogPriceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateCatalogPriceUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CatalogPriceDto execute(CreateCatalogPriceCommand command);
}
