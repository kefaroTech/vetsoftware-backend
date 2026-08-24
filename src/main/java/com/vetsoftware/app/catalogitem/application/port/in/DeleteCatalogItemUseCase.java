package com.vetsoftware.app.catalogitem.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteCatalogItemUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    void execute(Long id);
}
