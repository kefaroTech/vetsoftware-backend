package com.vetsoftware.app.servicechargeopenaccount.application.command;

public record CreateServiceChargeOpenAccountCommand(
        Long animalId,
        Long serviceId,
        Long openAccountId,
        Long companyId,
        Long createdById
) {}
