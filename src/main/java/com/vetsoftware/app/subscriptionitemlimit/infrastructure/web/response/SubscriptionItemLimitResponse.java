package com.vetsoftware.app.subscriptionitemlimit.infrastructure.web.response;

import com.vetsoftware.app.subscriptionitemlimit.application.dto.SubscriptionItemLimitDto;
import com.vetsoftware.app.subscriptionitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.subscriptionitemlimit.domain.LimitMode;
import com.vetsoftware.app.subscriptionitemlimit.domain.MeasureKind;
import com.vetsoftware.app.subscriptionitemlimit.domain.ResetPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * El techo congelado en la línea del contrato, tal como lo ven los frontends.
 *
 * <p>
 * {@code companyId} <strong>sale</strong> en la respuesta aunque no entre por
 * el cuerpo: la asimetría es la regla, no una incoherencia. Prohibido que el
 * cliente lo <em>escriba</em>, útil que lo <em>lea</em> —la consola de
 * plataforma lista los techos de una clínica concreta y necesita saber de quién
 * es cada fila—.
 */
public record SubscriptionItemLimitResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long subscriptionItemId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long limitDimensionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) MeasureKind measureKind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LimitMode mode, Integer limitQuantity,
        ResetPeriod resetPeriod,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LimitEnforcement enforcement,
        BigDecimal overageUnitAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int warnThreshold,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static SubscriptionItemLimitResponse from(SubscriptionItemLimitDto dto) {
        return new SubscriptionItemLimitResponse(dto.id(), dto.companyId(),
                dto.subscriptionItemId(), dto.limitDimensionId(), dto.measureKind(), dto.mode(),
                dto.limitQuantity(), dto.resetPeriod(), dto.enforcement(), dto.overageUnitAmount(),
                dto.warnThreshold(), dto.createdDate());
    }
}
