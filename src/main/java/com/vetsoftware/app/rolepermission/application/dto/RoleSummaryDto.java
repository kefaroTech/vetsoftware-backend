package com.vetsoftware.app.rolepermission.application.dto;

import com.vetsoftware.app.rolepermission.domain.RoleRef;

public record RoleSummaryDto(Long id, String name, String code) {
    public static RoleSummaryDto from(RoleRef ref) {
        return new RoleSummaryDto(ref.id(), ref.name(), ref.code());
    }
}
