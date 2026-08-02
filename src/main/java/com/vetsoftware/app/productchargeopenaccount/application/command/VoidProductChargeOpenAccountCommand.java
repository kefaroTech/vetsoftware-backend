package com.vetsoftware.app.productchargeopenaccount.application.command;

public record VoidProductChargeOpenAccountCommand(Long id, Long companyId, Long voidedById,
        String reason, Long expectedVersion) {
}
