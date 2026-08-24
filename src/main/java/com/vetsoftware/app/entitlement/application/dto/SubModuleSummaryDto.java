package com.vetsoftware.app.entitlement.application.dto;

import com.vetsoftware.app.entitlement.domain.SubModuleRef;

/** El submodulo al que apunta un permiso, tal como sale del caso de uso. */
public record SubModuleSummaryDto(Long id, String code, String name) {

    public static SubModuleSummaryDto from(SubModuleRef ref) {
        return new SubModuleSummaryDto(ref.id(), ref.code(), ref.name());
    }
}
