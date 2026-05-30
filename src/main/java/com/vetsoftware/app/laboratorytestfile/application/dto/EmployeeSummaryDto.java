package com.vetsoftware.app.laboratorytestfile.application.dto;

import com.vetsoftware.app.laboratorytestfile.domain.EmployeeRef;

public record EmployeeSummaryDto(Long id, String employeeCode, String name) {
    public static EmployeeSummaryDto from(EmployeeRef ref) {
        return new EmployeeSummaryDto(ref.id(), ref.employeeCode(), ref.name());
    }
}
