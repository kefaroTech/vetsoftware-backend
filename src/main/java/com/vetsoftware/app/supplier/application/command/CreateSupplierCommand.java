package com.vetsoftware.app.supplier.application.command;

public record CreateSupplierCommand(
        String name,
        String taxId,
        String contactName,
        String phone,
        String email,
        String address,
        Integer paymentTermsDays,
        String notes,
        Long companyId
) {}
