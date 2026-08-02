package com.vetsoftware.app.systempermission.application.dto;

import com.vetsoftware.app.systempermission.domain.SystemPermission;
import java.time.LocalDateTime;

public record SystemPermissionDto(Long id, String name, String code, LocalDateTime createdDate,
        boolean enabled) {
    public static SystemPermissionDto from(SystemPermission systemPermission) {
        return new SystemPermissionDto(systemPermission.getId(), systemPermission.getName(),
                systemPermission.getCode(), systemPermission.getCreatedDate(),
                systemPermission.isEnabled());
    }
}
