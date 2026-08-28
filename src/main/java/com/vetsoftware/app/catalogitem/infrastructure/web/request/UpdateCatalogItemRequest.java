package com.vetsoftware.app.catalogitem.infrastructure.web.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.vetsoftware.app.catalogitem.domain.CatalogItemStatus;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Sin {@code code}: el código de un artículo es inmutable por ficha. */
public record UpdateCatalogItemRequest(
        @NotBlank(message = "El nombre del artículo es obligatorio.") @Size(max = 120, message = "El nombre del artículo no puede superar los 120 caracteres.") String name,
        @Size(max = 255, message = "La descripción corta no puede superar los 255 caracteres.") String shortDescription,
        String longDescription,
        @NotNull(message = "Debes indicar el tipo de artículo.") ItemType itemType,
        @Size(max = 50, message = "El código de la unidad de capacidad no puede superar los 50 caracteres.") String capacityUnit,
        // Jackson 3 trae FAIL_ON_NULL_FOR_PRIMITIVES ACTIVADO (al reves que Jackson 2):
        // sin @JsonSetter, omitir la bandera responde 400 «Cannot map `null` into type
        // `boolean`» en vez de caer al default. Mismo patron que
        // CreateAppointmentRequest.forceOverlap.
        @JsonSetter(nulls = Nulls.AS_EMPTY) boolean core,
        @PositiveOrZero(message = "La cantidad mínima no puede ser negativa.") Integer minQuantity,
        @PositiveOrZero(message = "La cantidad máxima no puede ser negativa.") Integer maxQuantity,
        @PositiveOrZero(message = "El orden de presentación no puede ser negativo.") Integer sortOrder,
        @NotNull(message = "Debes indicar el estado del artículo.") CatalogItemStatus status) {
}
