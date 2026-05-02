package com.vetsoftware.app.systemuserpermission.application.dto;

import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermission;
import java.time.LocalDateTime;

public record SystemUserPermissionDto(Long id,
                                      SystemUserSummaryDto systemUser,
                                      SystemPermissionSummaryDto systemPermission,
                                      LocalDateTime createdDate) {
    public static SystemUserPermissionDto from(SystemUserPermission systemUserPermission) {
        return new SystemUserPermissionDto(
            systemUserPermission.getId(),
            SystemUserSummaryDto.from(systemUserPermission.getSystemUser()),
            SystemPermissionSummaryDto.from(systemUserPermission.getSystemPermission()),
            systemUserPermission.getCreatedDate()
        );
    }
}
