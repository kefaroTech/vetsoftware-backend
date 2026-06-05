package com.vetsoftware.app.openaccount.application.command;

public record UpdateOpenAccountCommand(
        Long id,
        Long ownerId,
        Long companyId
) {}
