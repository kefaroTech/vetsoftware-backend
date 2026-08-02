package com.vetsoftware.app.servicechargeopenaccount.application.command;

public record VoidServiceChargeOpenAccountCommand(Long id, Long companyId, Long voidedById,
        String reason, Long expectedVersion) {
}
