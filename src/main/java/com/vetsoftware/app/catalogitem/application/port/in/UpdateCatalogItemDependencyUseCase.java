package com.vetsoftware.app.catalogitem.application.port.in;

import com.vetsoftware.app.catalogitem.application.command.UpdateCatalogItemDependencyCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDependencyDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Cambiar el sentido de una regla también puede cerrar un ciclo: pasar de
 * {@code RECOMMENDS} a {@code REQUIRES} añade un arco al grafo que antes no
 * estaba. Por eso este puerto vuelve a pasar por el detector de R16 y no solo
 * el de alta.
 */
public interface UpdateCatalogItemDependencyUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CatalogItemDependencyDto execute(UpdateCatalogItemDependencyCommand command);
}
