package com.vetsoftware.app.productcategory.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProductCategoryRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio.") @Size(max = 100, message = "El nombre de la categoría no puede superar los 100 caracteres.") String name,
        @NotBlank(message = "La descripción es obligatoria.") @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.") String description,
        @NotNull(message = "No se pudo identificar la versión de la categoría. Vuelve a cargarla e inténtalo de nuevo.") Long version) {
}
