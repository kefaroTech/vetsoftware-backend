package com.vetsoftware.app.pricelist.application.port.in;

import com.vetsoftware.app.pricelist.application.command.UpdateCatalogPriceCommand;
import com.vetsoftware.app.pricelist.application.dto.CatalogPriceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateCatalogPriceUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CatalogPriceDto execute(UpdateCatalogPriceCommand command);
}
