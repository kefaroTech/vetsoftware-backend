package com.vetsoftware.app.openaccount.application.command;

public record CreateOpenAccountCommand(
        Long ownerId,
        Long companyId,
        Long createdById
) {}
