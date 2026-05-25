package com.vetsoftware.app.baserole.application.dto;

import com.vetsoftware.app.baserole.domain.BaseRole;
import java.time.LocalDateTime;

public record BaseRoleDto(Long id, String name, String code, Boolean mandatory, LocalDateTime createdDate, boolean enabled) {
    public static BaseRoleDto from(BaseRole baseRole) {
        return new BaseRoleDto(
            baseRole.getId(),
            baseRole.getName(),
            baseRole.getCode(),
            baseRole.getMandatory(),
            baseRole.getCreatedDate(),
            baseRole.isEnabled()
        );
    }
}
