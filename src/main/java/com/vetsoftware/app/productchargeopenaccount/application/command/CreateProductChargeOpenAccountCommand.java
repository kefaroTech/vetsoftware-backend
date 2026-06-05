package com.vetsoftware.app.productchargeopenaccount.application.command;

public record CreateProductChargeOpenAccountCommand(
        Long animalId,
        Long productId,
        Long openAccountId,
        Long companyId,
        Long createdById
) {}
