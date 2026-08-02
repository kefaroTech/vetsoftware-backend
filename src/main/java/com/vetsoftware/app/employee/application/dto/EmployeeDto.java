package com.vetsoftware.app.employee.application.dto;

import com.vetsoftware.app.employee.domain.BranchRef;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.RoleSnapshot;
import java.time.LocalDateTime;
import java.util.List;

public record EmployeeDto(Long id, String employeeCode, String name, String email,
        CompanySummaryDto company, List<RoleSummaryDto> roles, List<BranchSummaryDto> branches,
        LocalDateTime createdDate, boolean enabled, boolean mustChangePassword, String status) {
    public static EmployeeDto from(Employee employee) {
        return from(employee, List.of(), List.of());
    }

    public static EmployeeDto from(Employee employee, List<RoleSnapshot> roles) {
        return from(employee, roles, List.of());
    }

    public static EmployeeDto from(Employee employee, List<RoleSnapshot> roles,
            List<BranchRef> branches) {
        return new EmployeeDto(employee.getId(), employee.getEmployeeCode(), employee.getName(),
                employee.getEmail(), CompanySummaryDto.from(employee.getCompany()),
                roles.stream().map(RoleSummaryDto::from).toList(),
                branches.stream().map(BranchSummaryDto::from).toList(), employee.getCreatedDate(),
                employee.isEnabled(), employee.isMustChangePassword(), employee.getStatus().name());
    }
}
