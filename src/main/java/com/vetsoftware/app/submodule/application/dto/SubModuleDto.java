package com.vetsoftware.app.submodule.application.dto;

import com.vetsoftware.app.submodule.domain.SubModule;
import java.time.LocalDateTime;

public record SubModuleDto(Long id, String name, String code, ModuleSummaryDto module, LocalDateTime createdDate, boolean enabled) {
    public static SubModuleDto from(SubModule subModule) {
        return new SubModuleDto(
            subModule.getId(),
            subModule.getName(),
            subModule.getCode(),
            ModuleSummaryDto.from(subModule.getModule()),
            subModule.getCreatedDate(),
            subModule.isEnabled()
        );
    }
}
