package com.vetsoftware.app.catalogitem.application.port.in;

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemDependencyCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDependencyDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Alta de una regla del configurador. Es el puerto que implementa la
 * <strong>regla R16</strong>: si el arco {@code REQUIRES} que se pide cierra un
 * ciclo indirecto, se rechaza con {@code CatalogItemDependencyCycleException}
 * antes de escribir nada.
 */
public interface CreateCatalogItemDependencyUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CatalogItemDependencyDto execute(CreateCatalogItemDependencyCommand command);
}
