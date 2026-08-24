package com.vetsoftware.app.configurator.application.dto;

import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.EffectType;
import java.time.LocalDateTime;

public record ConfiguratorEffectDto(Long id, Long optionId, Long questionId, Long catalogItemId,
        EffectType effect, Integer quantity, LocalDateTime createdDate, boolean enabled) {

    public static ConfiguratorEffectDto from(ConfiguratorEffect effect) {
        return new ConfiguratorEffectDto(effect.getId(), effect.getOptionId(),
                effect.getQuestionId(), effect.getCatalogItemId(), effect.getEffect(),
                effect.getQuantity(), effect.getCreatedDate(), effect.isEnabled());
    }
}
