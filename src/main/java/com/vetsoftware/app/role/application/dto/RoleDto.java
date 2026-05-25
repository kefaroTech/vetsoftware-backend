package com.vetsoftware.app.role.application.dto;

import com.vetsoftware.app.role.domain.Role;
import java.time.LocalDateTime;
import java.util.List;

public record RoleDto(Long id, String name, String code,
                      CompanySummaryDto company,
                      LocalDateTime createdDate,
                      List<PermissionSummaryDto> permissions,
                      boolean enabled) {

    public static RoleDto from(Role role) {
        return from(role, List.of());
    }

    public static RoleDto from(Role role, List<PermissionSummaryDto> permissions) {
        return new RoleDto(
            role.getId(),
            role.getName(),
            role.getCode(),
            CompanySummaryDto.from(role.getCompany()),
            role.getCreatedDate(),
            permissions,
            role.isEnabled()
        );
    }
}
