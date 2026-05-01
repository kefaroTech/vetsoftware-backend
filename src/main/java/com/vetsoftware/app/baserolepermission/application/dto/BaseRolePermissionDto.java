package com.vetsoftware.app.baserolepermission.application.dto;

import com.vetsoftware.app.baserolepermission.domain.BaseRolePermission;
import java.time.LocalDateTime;

public record BaseRolePermissionDto(Long id, Long baseRoleId, Long basePermissionId, LocalDateTime createdDate) {
    public static BaseRolePermissionDto from(BaseRolePermission baseRolePermission) {
        return new BaseRolePermissionDto(
            baseRolePermission.getId(),
            baseRolePermission.getBaseRoleId(),
            baseRolePermission.getBasePermissionId(),
            baseRolePermission.getCreatedDate()
        );
    }
}
