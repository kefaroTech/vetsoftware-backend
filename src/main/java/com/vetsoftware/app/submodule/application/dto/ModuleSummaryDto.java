package com.vetsoftware.app.submodule.application.dto;

import com.vetsoftware.app.submodule.domain.ModuleRef;

public record ModuleSummaryDto(Long id, String name, String code) {
    public static ModuleSummaryDto from(ModuleRef ref) {
        return new ModuleSummaryDto(ref.id(), ref.name(), ref.code());
    }
}
