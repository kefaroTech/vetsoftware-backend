package com.vetsoftware.app.servicechargeopenaccount.application.dto;

import com.vetsoftware.app.servicechargeopenaccount.domain.EmployeeRef;

public record EmployeeSummaryDto(Long id, String name) {
    public static EmployeeSummaryDto from(EmployeeRef employee) {
        return new EmployeeSummaryDto(employee.id(), employee.name());
    }
}
