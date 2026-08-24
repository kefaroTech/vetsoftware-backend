package com.vetsoftware.app.catalogitem.infrastructure.web.request;

import com.vetsoftware.app.catalogitem.domain.RelationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCatalogItemDependencyRequest(
        @NotNull(message = "Debes seleccionar el artículo relacionado.") Long relatedItemId,
        @NotNull(message = "Debes indicar el tipo de relación.") RelationType relationType,
        @Size(max = 255, message = "La nota no puede superar los 255 caracteres.") String note) {
}
