package com.vetsoftware.app.spa.application.command;

public record ChangeSpaStatusCommand(
        Long id,
        String status,
        Long companyId
) {}
