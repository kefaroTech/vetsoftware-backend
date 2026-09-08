package com.vetsoftware.app.subscriptionmodule.infrastructure.web.response;

import com.vetsoftware.app.subscriptionmodule.application.dto.ModuleCeilingDto;

public record ModuleCeilingResponse(String dimensionCode, String measureKind, int used, int limit,
        int warnThreshold, String enforcement) {

    public static ModuleCeilingResponse from(ModuleCeilingDto dto) {
        return new ModuleCeilingResponse(dto.dimensionCode(), dto.measureKind(), dto.used(),
                dto.limit(), dto.warnThreshold(), dto.enforcement());
    }
}
