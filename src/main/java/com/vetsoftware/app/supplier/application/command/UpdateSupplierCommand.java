package com.vetsoftware.app.supplier.application.command;

public record UpdateSupplierCommand(Long id, String name, String taxId, String contactName,
        String phone, String email, String address, Integer paymentTermsDays, String notes,
        Long companyId, Long updatedBy, Long version) {
}
