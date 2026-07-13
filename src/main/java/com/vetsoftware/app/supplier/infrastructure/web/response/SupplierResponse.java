package com.vetsoftware.app.supplier.infrastructure.web.response;

import java.time.LocalDateTime;

public record SupplierResponse(
        Long id,
        String name,
        String taxId,
        String contactName,
        String phone,
        String email,
        String address,
        Integer paymentTermsDays,
        String notes,
        CompanySummary company,
        LocalDateTime createdDate,
        LocalDateTime updatedDate,
        Long updatedBy,
        Long version,
        boolean enabled
) {}
