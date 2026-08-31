package com.vetsoftware.app.catalogitemaihint.application.port.in;

import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El historial completo de un articulo, de la revision mas nueva a la mas
 * vieja.
 *
 * <p>
 * Es lo que hace util al diseno append-only de la tabla: sin esta lectura, la
 * revision reemplazada quedaria guardada y no la podria ver nadie, que a
 * efectos practicos es lo mismo que haberla borrado.
 */
public interface ListCatalogItemAiHintRevisionsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<CatalogItemAiHintDto> listByCatalogItemId(Long catalogItemId, int page,
            int pageSize);
}
