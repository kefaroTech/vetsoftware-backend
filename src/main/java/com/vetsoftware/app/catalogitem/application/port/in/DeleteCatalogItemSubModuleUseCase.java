package com.vetsoftware.app.catalogitem.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Recibe también el artículo padre porque el endpoint está anidado: el caso de
 * uso comprueba que el vínculo que se retira cuelga de ese artículo y no de
 * otro. No es aislamiento de tenant —aquí no hay tenant— sino coherencia de la
 * ruta: sin ello un id escrito a mano borra el vínculo de un artículo distinto
 * del que dice la URL, y el 404 que devolvería el cliente sería mentira.
 */
public interface DeleteCatalogItemSubModuleUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    void execute(Long catalogItemId, Long id);
}
