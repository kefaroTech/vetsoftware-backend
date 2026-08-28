package com.vetsoftware.app.catalogitemlimit.application.port.in;

import com.vetsoftware.app.catalogitemlimit.application.command.UpdateCatalogItemLimitCommand;
import com.vetsoftware.app.catalogitemlimit.application.dto.CatalogItemLimitDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Cambia el techo de fábrica.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas. El command lleva un
 * {@code id} que el cliente escribe, que es lo que vigila la familia «por id»;
 * como la fila no pertenece a ninguna empresa, la salida correcta es cerrar la
 * operación a un principal cross-tenant.
 */
public interface UpdateCatalogItemLimitUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CatalogItemLimitDto execute(UpdateCatalogItemLimitCommand command);
}
