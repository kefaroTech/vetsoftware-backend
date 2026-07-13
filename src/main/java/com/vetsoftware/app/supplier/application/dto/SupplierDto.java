package com.vetsoftware.app.supplier.application.dto;

import com.vetsoftware.app.supplier.domain.Supplier;
import java.time.LocalDateTime;

public record SupplierDto(
        Long id,
        String name,
        String taxId,
        String contactName,
        String phone,
        String email,
        String address,
        Integer paymentTermsDays,
        String notes,
        CompanySummaryDto company,
        LocalDateTime createdDate,
        LocalDateTime updatedDate,
        Long updatedBy,
        Long version,
        boolean enabled
) {
    public static SupplierDto from(Supplier supplier) {
        return new SupplierDto(
                supplier.getId(),
                supplier.getName(),
                supplier.getTaxId(),
                supplier.getContactName(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.getAddress(),
                supplier.getPaymentTermsDays(),
                supplier.getNotes(),
                CompanySummaryDto.from(supplier.getCompany()),
                supplier.getCreatedDate(),
                supplier.getUpdatedDate(),
                supplier.getUpdatedBy(),
                supplier.getVersion(),
                supplier.isEnabled());
    }
}
