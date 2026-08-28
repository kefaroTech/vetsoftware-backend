package com.vetsoftware.app.uvtvalue.infrastructure.web.response;

import com.vetsoftware.app.uvtvalue.application.dto.UvtValueDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UvtValueResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2026") int fiscalYear,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "52374.00") BigDecimal valueAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Resolucion DIAN 000238 del 15-12-2025") String legalReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {

    public static UvtValueResponse from(UvtValueDto dto) {
        return new UvtValueResponse(dto.id(), dto.fiscalYear(), dto.valueAmount(),
                dto.legalReference(), dto.createdDate(), dto.enabled());
    }
}
