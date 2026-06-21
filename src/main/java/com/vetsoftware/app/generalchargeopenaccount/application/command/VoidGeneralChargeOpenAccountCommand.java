package com.vetsoftware.app.generalchargeopenaccount.application.command;

public record VoidGeneralChargeOpenAccountCommand(
        Long id, Long companyId, Long voidedById, String reason, Long expectedVersion) {}
