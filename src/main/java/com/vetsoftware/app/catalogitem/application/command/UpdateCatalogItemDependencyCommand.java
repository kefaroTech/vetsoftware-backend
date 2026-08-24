package com.vetsoftware.app.catalogitem.application.command;

import com.vetsoftware.app.catalogitem.domain.RelationType;

/**
 * Lleva {@code catalogItemId} ademas del {@code id} de la dependencia porque el
 * endpoint esta anidado bajo el articulo: el caso de uso comprueba que la fila
 * que se edita cuelga de ese padre y no de otro. Sin esa comprobacion, un
 * {@code id} escrito a mano en la URL edita la regla de un articulo distinto
 * del que dice la ruta.
 */
public record UpdateCatalogItemDependencyCommand(Long id, Long catalogItemId,
        RelationType relationType, String note) {
}
