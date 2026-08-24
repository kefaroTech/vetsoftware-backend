package com.vetsoftware.app.catalogitem.application.port.in;

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemSubModuleCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemSubModuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateCatalogItemSubModuleUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CatalogItemSubModuleDto execute(CreateCatalogItemSubModuleCommand command);
}
