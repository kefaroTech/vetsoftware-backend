package com.vetsoftware.app.configurator.domain;

public class ConfiguratorQuestionNotFoundException extends RuntimeException {
    public ConfiguratorQuestionNotFoundException(Long id) {
        super("ConfiguratorQuestion not found: " + id);
    }
}
