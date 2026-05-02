package com.vetsoftware.app.employeerole.application.dto;

import com.vetsoftware.app.employeerole.domain.EmployeeRef;

public record EmployeeSummaryDto(Long id, String employeeCode, String name) {
    public static EmployeeSummaryDto from(EmployeeRef ref) {
        return new EmployeeSummaryDto(ref.id(), ref.employeeCode(), ref.name());
    }
}
