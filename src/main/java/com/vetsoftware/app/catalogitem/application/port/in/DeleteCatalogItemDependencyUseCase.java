package com.vetsoftware.app.catalogitem.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteCatalogItemDependencyUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    void execute(Long catalogItemId, Long id);
}
