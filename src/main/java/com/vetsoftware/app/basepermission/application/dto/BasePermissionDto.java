package com.vetsoftware.app.basepermission.application.dto;

import com.vetsoftware.app.basepermission.domain.BasePermission;
import java.time.LocalDateTime;

public record BasePermissionDto(Long id, String name, String code, SubModuleSummaryDto subModule, LocalDateTime createdDate) {
    public static BasePermissionDto from(BasePermission basePermission) {
        return new BasePermissionDto(
            basePermission.getId(),
            basePermission.getName(),
            basePermission.getCode(),
            SubModuleSummaryDto.from(basePermission.getSubModule()),
            basePermission.getCreatedDate()
        );
    }
}
