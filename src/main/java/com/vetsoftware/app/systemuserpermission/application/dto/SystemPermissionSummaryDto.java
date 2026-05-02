package com.vetsoftware.app.systemuserpermission.application.dto;

import com.vetsoftware.app.systemuserpermission.domain.SystemPermissionRef;

public record SystemPermissionSummaryDto(Long id, String name, String code) {
    public static SystemPermissionSummaryDto from(SystemPermissionRef ref) {
        return new SystemPermissionSummaryDto(ref.id(), ref.name(), ref.code());
    }
}
