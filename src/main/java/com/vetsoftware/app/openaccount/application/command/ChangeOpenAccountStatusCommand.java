package com.vetsoftware.app.openaccount.application.command;

public record ChangeOpenAccountStatusCommand(
        Long id,
        String status,
        Long employeeId,
        String reason
) {}
