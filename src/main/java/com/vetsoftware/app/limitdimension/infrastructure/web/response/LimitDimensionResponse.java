package com.vetsoftware.app.limitdimension.infrastructure.web.response;

import com.vetsoftware.app.limitdimension.application.dto.LimitDimensionDto;
import com.vetsoftware.app.limitdimension.domain.MeasureKind;
import com.vetsoftware.app.limitdimension.domain.SubModuleRef;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un eje limitable tal como lo ven los frontends.
 *
 * <p>
 * {@code availableFrom} viaja y no es metadato de auditoría: es la mitad de la
 * respuesta a «este cliente no tiene contador porque no se le vendió» frente a
 * «no lo tiene porque el eje no existía cuando firmó» (D-74). Una pantalla que
 * no lo reciba no puede distinguir las dos, y son opuestas.
 */
public record LimitDimensionResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) MeasureKind measureKind,
        SubModuleSummary subModule, Integer releaseDelayDays,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate availableFrom,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static LimitDimensionResponse from(LimitDimensionDto dto) {
        SubModuleRef subModule = dto.subModule();
        return new LimitDimensionResponse(dto.id(), dto.code(), dto.name(), dto.measureKind(),
                subModule == null
                        ? null
                        : new SubModuleSummary(subModule.id(), subModule.code(), subModule.name()),
                dto.releaseDelayDays(), dto.availableFrom(), dto.createdDate());
    }
}
