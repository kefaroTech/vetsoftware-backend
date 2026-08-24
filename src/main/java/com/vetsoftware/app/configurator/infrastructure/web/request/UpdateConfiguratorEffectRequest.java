package com.vetsoftware.app.configurator.infrastructure.web.request;

import com.vetsoftware.app.configurator.domain.EffectType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Sin disparador: el {@code optionId} / {@code questionId} de un efecto no se
 * edita.
 */
public record UpdateConfiguratorEffectRequest(
        @NotNull(message = "Debes seleccionar el artículo del catálogo.") Long catalogItemId,
        @NotNull(message = "Debes seleccionar el efecto.") EffectType effect,
        @Positive(message = "La cantidad debe ser mayor que cero.") Integer quantity) {
}
