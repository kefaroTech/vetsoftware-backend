package com.vetsoftware.app.supplier.application.command;

public record SearchSuppliersCommand(Long companyId, String name, String taxId, int page,
        int pageSize) {
}
