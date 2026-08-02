package com.vetsoftware.app.surgery.application.command;

public record ChangeSurgeryStatusCommand(
        Long id,
        String status,
        Long companyId
) {}
