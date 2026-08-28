package com.vetsoftware.app.catalogitemlimit.infrastructure.web.response;

import com.vetsoftware.app.catalogitemlimit.application.dto.CatalogItemLimitDto;
import com.vetsoftware.app.catalogitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.catalogitemlimit.domain.LimitMode;
import com.vetsoftware.app.catalogitemlimit.domain.MeasureKind;
import com.vetsoftware.app.catalogitemlimit.domain.ResetPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * El techo de fábrica tal como lo ven los frontends.
 *
 * <p>
 * {@code measureKind} viaja aunque no sea editable: es la copia que el motor
 * ata a {@code limit_dimensions(id, measure_kind)}, y es lo que le dice a la
 * pantalla si el campo «cada cuánto se reinicia» tiene sentido o no para este
 * techo.
 */
public record CatalogItemLimitResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long catalogItemId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long limitDimensionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) MeasureKind measureKind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LimitMode mode, Integer limitQuantity,
        ResetPeriod resetPeriod,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LimitEnforcement enforcement,
        BigDecimal overageUnitAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int warnThreshold,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LimitMode trialMode,
        Integer trialLimitQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static CatalogItemLimitResponse from(CatalogItemLimitDto dto) {
        return new CatalogItemLimitResponse(dto.id(), dto.catalogItemId(), dto.limitDimensionId(),
                dto.measureKind(), dto.mode(), dto.limitQuantity(), dto.resetPeriod(),
                dto.enforcement(), dto.overageUnitAmount(), dto.warnThreshold(), dto.trialMode(),
                dto.trialLimitQuantity(), dto.createdDate());
    }
}
