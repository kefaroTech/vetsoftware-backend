package com.vetsoftware.app.baserolepermission.application.dto;

import com.vetsoftware.app.baserolepermission.domain.BaseRolePermission;
import java.time.LocalDateTime;

public record BaseRolePermissionDto(Long id,
                                     BaseRoleSummaryDto baseRole,
                                     BasePermissionSummaryDto basePermission,
                                     LocalDateTime createdDate) {
    public static BaseRolePermissionDto from(BaseRolePermission baseRolePermission) {
        return new BaseRolePermissionDto(
            baseRolePermission.getId(),
            BaseRoleSummaryDto.from(baseRolePermission.getBaseRole()),
            BasePermissionSummaryDto.from(baseRolePermission.getBasePermission()),
            baseRolePermission.getCreatedDate()
        );
    }
}
