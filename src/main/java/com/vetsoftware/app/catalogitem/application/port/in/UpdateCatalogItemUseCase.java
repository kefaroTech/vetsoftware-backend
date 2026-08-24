package com.vetsoftware.app.catalogitem.application.port.in;

import com.vetsoftware.app.catalogitem.application.command.UpdateCatalogItemCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateCatalogItemUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CatalogItemDto execute(UpdateCatalogItemCommand command);
}
