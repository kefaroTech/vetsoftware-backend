package com.vetsoftware.app.employeerole.application.dto;

import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import java.time.LocalDateTime;

public record EmployeeRoleDto(Long id,
                              EmployeeSummaryDto employee,
                              RoleSummaryDto role,
                              LocalDateTime createdDate,
                              boolean enabled) {
    public static EmployeeRoleDto from(EmployeeRole employeeRole) {
        return new EmployeeRoleDto(
            employeeRole.getId(),
            EmployeeSummaryDto.from(employeeRole.getEmployee()),
            RoleSummaryDto.from(employeeRole.getRole()),
            employeeRole.getCreatedDate(),
            employeeRole.isEnabled()
        );
    }
}
