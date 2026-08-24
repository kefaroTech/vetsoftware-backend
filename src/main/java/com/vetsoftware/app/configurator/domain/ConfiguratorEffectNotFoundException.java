package com.vetsoftware.app.configurator.domain;

public class ConfiguratorEffectNotFoundException extends RuntimeException {
    public ConfiguratorEffectNotFoundException(Long id) {
        super("ConfiguratorEffect not found: " + id);
    }
}
