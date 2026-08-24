package com.vetsoftware.app.catalogitem.application.port.in;

import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateCatalogItemUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CatalogItemDto execute(Long id);
}
