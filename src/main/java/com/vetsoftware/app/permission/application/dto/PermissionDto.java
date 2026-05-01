package com.vetsoftware.app.permission.application.dto;

import com.vetsoftware.app.permission.domain.Permission;
import java.time.LocalDateTime;

public record PermissionDto(Long id, String name, String code, Long companyId, Long subModuleId, LocalDateTime createdDate) {
    public static PermissionDto from(Permission permission) {
        return new PermissionDto(
            permission.getId(),
            permission.getName(),
            permission.getCode(),
            permission.getCompanyId(),
            permission.getSubModuleId(),
            permission.getCreatedDate()
        );
    }
}
