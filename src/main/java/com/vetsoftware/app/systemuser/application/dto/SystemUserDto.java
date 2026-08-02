package com.vetsoftware.app.systemuser.application.dto;

import com.vetsoftware.app.systemuser.domain.SystemUser;
import java.time.LocalDateTime;

public record SystemUserDto(Long id, String code, LocalDateTime createdDate, boolean enabled) {
    public static SystemUserDto from(SystemUser systemUser) {
        return new SystemUserDto(systemUser.getId(), systemUser.getCode(),
                systemUser.getCreatedDate(), systemUser.isEnabled());
    }
}
