package com.vetsoftware.app.catalogitem.application.port.in;

import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDependencyDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListCatalogItemDependenciesUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    List<CatalogItemDependencyDto> listByCatalogItem(Long catalogItemId);
}
