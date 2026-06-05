package com.vetsoftware.app.productchargeopenaccount.application.command;

public record UpdateProductChargeOpenAccountCommand(
        Long id,
        Long animalId,
        Long productId,
        Long openAccountId,
        Long companyId
) {}
