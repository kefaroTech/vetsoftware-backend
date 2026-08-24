package com.vetsoftware.app.catalogitem.application.port.in;

import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindCatalogItemUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CatalogItemDto findById(Long id);
}
