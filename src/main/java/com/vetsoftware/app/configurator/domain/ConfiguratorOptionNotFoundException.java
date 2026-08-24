package com.vetsoftware.app.configurator.domain;

public class ConfiguratorOptionNotFoundException extends RuntimeException {
    public ConfiguratorOptionNotFoundException(Long id) {
        super("ConfiguratorOption not found: " + id);
    }
}
