package com.vetsoftware.app.openaccount.application.command;

public record SearchOpenAccountsCommand(
        Long companyId,
        Long ownerId,
        Boolean enabled,
        int page,
        int pageSize
) {}
