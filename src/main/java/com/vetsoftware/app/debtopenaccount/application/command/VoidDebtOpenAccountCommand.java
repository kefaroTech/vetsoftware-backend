package com.vetsoftware.app.debtopenaccount.application.command;

public record VoidDebtOpenAccountCommand(
    Long id, Long companyId, Long voidedById, String reason, Long expectedVersion) {}
