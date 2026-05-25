package com.vetsoftware.app.employee.application.dto;

import com.vetsoftware.app.employee.domain.RoleSnapshot;

public record RoleSummaryDto(Long employeeRoleId, Long id, String name, String code) {
    public static RoleSummaryDto from(RoleSnapshot s) {
        return new RoleSummaryDto(s.employeeRoleId(), s.id(), s.name(), s.code());
    }
}
