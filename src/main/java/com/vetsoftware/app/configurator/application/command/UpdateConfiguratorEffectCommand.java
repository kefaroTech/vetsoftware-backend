package com.vetsoftware.app.configurator.application.command;

import com.vetsoftware.app.configurator.domain.EffectType;

public record UpdateConfiguratorEffectCommand(Long id, Long catalogItemId, EffectType effect,
        Integer quantity) {
}
