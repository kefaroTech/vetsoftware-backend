package com.vetsoftware.app.employee.application.dto;

import com.vetsoftware.app.employee.domain.Employee;
import java.time.LocalDateTime;

public record EmployeeDto(Long id, String employeeCode, String name, String email,
                          String status, CompanySummaryDto company,
                          LocalDateTime createdDate) {
    public static EmployeeDto from(Employee employee) {
        return new EmployeeDto(
            employee.getId(),
            employee.getEmployeeCode(),
            employee.getName(),
            employee.getEmail(),
            employee.getStatus().name(),
            CompanySummaryDto.from(employee.getCompany()),
            employee.getCreatedDate()
        );
    }
}
