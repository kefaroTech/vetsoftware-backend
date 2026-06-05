package com.vetsoftware.app.productchargeopenaccount.application.dto;

import com.vetsoftware.app.productchargeopenaccount.domain.EmployeeRef;

public record EmployeeSummaryDto(Long id, String name) {
    public static EmployeeSummaryDto from(EmployeeRef employee) {
        return new EmployeeSummaryDto(employee.id(), employee.name());
    }
}
