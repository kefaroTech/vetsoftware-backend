package com.vetsoftware.app.catalogitem.application.port.in;

import com.vetsoftware.app.catalogitem.application.dto.CatalogItemSubModuleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListCatalogItemSubModulesUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    List<CatalogItemSubModuleDto> listByCatalogItem(Long catalogItemId);
}
