package com.vetsoftware.app.servicechargeopenaccount.application.command;

public record UpdateServiceChargeOpenAccountCommand(
        Long id,
        Long animalId,
        Long serviceId,
        Long openAccountId,
        Long companyId
) {}
