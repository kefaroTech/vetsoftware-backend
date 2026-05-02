package com.vetsoftware.app.permission.application.dto;

import com.vetsoftware.app.permission.domain.Permission;
import java.time.LocalDateTime;

public record PermissionDto(Long id, String name, String code,
                            CompanySummaryDto company, SubModuleSummaryDto subModule,
                            LocalDateTime createdDate) {
    public static PermissionDto from(Permission permission) {
        return new PermissionDto(
            permission.getId(),
            permission.getName(),
            permission.getCode(),
            CompanySummaryDto.from(permission.getCompany()),
            SubModuleSummaryDto.from(permission.getSubModule()),
            permission.getCreatedDate()
        );
    }
}
