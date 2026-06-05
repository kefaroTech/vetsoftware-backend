package com.vetsoftware.app.debtopenaccount.application.dto;

import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;

public record EmployeeSummaryDto(Long id, String name) {
    public static EmployeeSummaryDto from(EmployeeRef createdBy) {
        return new EmployeeSummaryDto(createdBy.id(), createdBy.name());
    }
}
