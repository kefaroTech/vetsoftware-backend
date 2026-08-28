package com.vetsoftware.app.configurator.application.dto;

import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.EffectType;
import java.time.LocalDateTime;

/**
 * @param priority
 *            el orden de aplicación, ascendente. <strong>Se publica</strong>,
 *            al contrario que la {@code version}: no es una barandilla técnica
 *            sino el dato que la pantalla de reordenado necesita para pintar el
 *            orden actual y para mandar el siguiente.
 */
public record ConfiguratorEffectDto(Long id, Long optionId, Long questionId, Long catalogItemId,
        EffectType effect, Integer quantity, int priority, LocalDateTime createdDate,
        boolean enabled) {

    public static ConfiguratorEffectDto from(ConfiguratorEffect effect) {
        return new ConfiguratorEffectDto(effect.getId(), effect.getOptionId(),
                effect.getQuestionId(), effect.getCatalogItemId(), effect.getEffect(),
                effect.getQuantity(), effect.getPriority(), effect.getCreatedDate(),
                effect.isEnabled());
    }
}
