package com.vetsoftware.app.catalogitemlimit.application.dto;

import com.vetsoftware.app.catalogitemlimit.domain.CatalogItemLimit;
import com.vetsoftware.app.catalogitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.catalogitemlimit.domain.LimitMode;
import com.vetsoftware.app.catalogitemlimit.domain.MeasureKind;
import com.vetsoftware.app.catalogitemlimit.domain.ResetPeriod;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** El techo de fábrica tal como sale de la feature. */
public record CatalogItemLimitDto(Long id, Long catalogItemId, Long limitDimensionId,
        MeasureKind measureKind, LimitMode mode, Integer limitQuantity, ResetPeriod resetPeriod,
        LimitEnforcement enforcement, BigDecimal overageUnitAmount, int warnThreshold,
        LimitMode trialMode, Integer trialLimitQuantity, LocalDateTime createdDate) {

    public static CatalogItemLimitDto from(CatalogItemLimit limit) {
        return new CatalogItemLimitDto(limit.getId(), limit.getCatalogItemId(),
                limit.getLimitDimensionId(), limit.getMeasureKind(), limit.getMode(),
                limit.getLimitQuantity(), limit.getResetPeriod(), limit.getEnforcement(),
                limit.getOverageUnitAmount(), limit.getWarnThreshold(), limit.getTrialMode(),
                limit.getTrialLimitQuantity(), limit.getCreatedDate());
    }
}
