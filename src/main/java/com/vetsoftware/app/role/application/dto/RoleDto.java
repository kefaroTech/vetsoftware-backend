package com.vetsoftware.app.role.application.dto;

import com.vetsoftware.app.role.domain.Role;
import java.time.LocalDateTime;

public record RoleDto(Long id, String name, String code,
                      CompanySummaryDto company,
                      LocalDateTime createdDate) {
    public static RoleDto from(Role role) {
        return new RoleDto(
            role.getId(),
            role.getName(),
            role.getCode(),
            CompanySummaryDto.from(role.getCompany()),
            role.getCreatedDate()
        );
    }
}
