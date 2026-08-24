package com.vetsoftware.app.configurator.application.command;

public record CreateConfiguratorOptionCommand(Long questionId, String code, String label,
        String helpText, int sortOrder) {
}
