package com.vetsoftware.app.submodule.application.dto;

import com.vetsoftware.app.submodule.domain.SubModule;
import java.time.LocalDateTime;

public record SubModuleDto(Long id, String name, String code, Long moduleId, LocalDateTime createdDate) {
    public static SubModuleDto from(SubModule subModule) {
        return new SubModuleDto(
            subModule.getId(),
            subModule.getName(),
            subModule.getCode(),
            subModule.getModuleId(),
            subModule.getCreatedDate()
        );
    }
}
