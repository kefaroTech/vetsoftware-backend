package com.vetsoftware.app.rolepermission.application.dto;

import com.vetsoftware.app.rolepermission.domain.RolePermission;
import java.time.LocalDateTime;

public record RolePermissionDto(Long id, RoleSummaryDto role, PermissionSummaryDto permission,
        LocalDateTime createdDate, boolean enabled) {
    public static RolePermissionDto from(RolePermission rolePermission) {
        return new RolePermissionDto(rolePermission.getId(),
                RoleSummaryDto.from(rolePermission.getRole()),
                PermissionSummaryDto.from(rolePermission.getPermission()),
                rolePermission.getCreatedDate(), rolePermission.isEnabled());
    }
}
