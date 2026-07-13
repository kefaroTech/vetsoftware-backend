package com.vetsoftware.app.supplier.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSupplierRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 30) String taxId,
        @Size(max = 100) String contactName,
        @Size(max = 30) String phone,
        @Size(max = 150) String email,
        @Size(max = 200) String address,
        @Min(0) Integer paymentTermsDays,
        @Size(max = 500) String notes,
        @NotNull Long version
) {}
