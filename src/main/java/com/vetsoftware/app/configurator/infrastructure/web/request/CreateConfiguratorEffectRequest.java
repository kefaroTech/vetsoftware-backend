package com.vetsoftware.app.configurator.infrastructure.web.request;

import com.vetsoftware.app.configurator.domain.EffectType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * {@code optionId} y {@code questionId} son opcionales <em>por separado</em>
 * pero exactamente uno tiene que venir. Eso no es una restricción de campo —es
 * una relación entre dos— así que no se puede escribir con anotaciones sueltas
 * y vive donde vive el resto de invariantes: en el constructor de
 * {@code ConfiguratorEffect}.
 */
public record CreateConfiguratorEffectRequest(Long optionId, Long questionId,
        @NotNull(message = "Debes seleccionar el artículo del catálogo.") Long catalogItemId,
        @NotNull(message = "Debes seleccionar el efecto.") EffectType effect,
        @Positive(message = "La cantidad debe ser mayor que cero.") Integer quantity) {
}
