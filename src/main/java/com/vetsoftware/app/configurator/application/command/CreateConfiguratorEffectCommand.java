package com.vetsoftware.app.configurator.application.command;

import com.vetsoftware.app.configurator.domain.EffectType;

/**
 * Exactamente uno de {@code optionId} y {@code questionId} viene relleno. La
 * invariante vive en {@code ConfiguratorEffect}, no aquí: un command es una
 * bolsa de datos y validarla en dos sitios es cómo acaban discrepando.
 */
public record CreateConfiguratorEffectCommand(Long optionId, Long questionId, Long catalogItemId,
        EffectType effect, Integer quantity) {
}
