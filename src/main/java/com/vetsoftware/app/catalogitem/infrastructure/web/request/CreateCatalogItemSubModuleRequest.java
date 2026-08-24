package com.vetsoftware.app.catalogitem.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record CreateCatalogItemSubModuleRequest(
        @NotNull(message = "Debes seleccionar el submódulo.") Long subModuleId) {
}
