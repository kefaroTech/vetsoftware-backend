package com.vetsoftware.app.generalchargeopenaccount.application.dto;

import com.vetsoftware.app.generalchargeopenaccount.domain.EmployeeRef;

public record EmployeeSummaryDto(Long id, String name) {
    public static EmployeeSummaryDto from(EmployeeRef employee) {
        return new EmployeeSummaryDto(employee.id(), employee.name());
    }
}
