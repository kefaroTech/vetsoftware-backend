package com.vetsoftware.app.catalogitem.infrastructure.web.request;

import com.vetsoftware.app.catalogitem.domain.RelationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCatalogItemDependencyRequest(
        @NotNull(message = "Debes indicar el tipo de relación.") RelationType relationType,
        @Size(max = 255, message = "La nota no puede superar los 255 caracteres.") String note) {
}
