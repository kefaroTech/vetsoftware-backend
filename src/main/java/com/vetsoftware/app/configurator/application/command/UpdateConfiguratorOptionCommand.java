package com.vetsoftware.app.configurator.application.command;

public record UpdateConfiguratorOptionCommand(Long id, String label, String helpText,
        int sortOrder) {
}
