package com.vetsoftware.app.service.application.command;

import java.math.BigDecimal;

public record CreateServiceCommand(
        String name,
        BigDecimal price,
        boolean hasTax,
        String notes,
        Long serviceCategoryId,
        Long taxId,
        Long companyId
) {}
