package com.vetsoftware.app.module.application.dto;

import com.vetsoftware.app.module.domain.Module;
import java.time.LocalDateTime;

public record ModuleDto(Long id, String name, String code, LocalDateTime createdDate) {
    public static ModuleDto from(Module module) {
        return new ModuleDto(module.getId(), module.getName(), module.getCode(), module.getCreatedDate());
    }
}
