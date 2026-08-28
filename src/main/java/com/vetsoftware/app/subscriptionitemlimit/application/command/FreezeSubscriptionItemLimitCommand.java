package com.vetsoftware.app.subscriptionitemlimit.application.command;

import com.vetsoftware.app.subscriptionitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.subscriptionitemlimit.domain.LimitMode;
import com.vetsoftware.app.subscriptionitemlimit.domain.MeasureKind;
import com.vetsoftware.app.subscriptionitemlimit.domain.ResetPeriod;
import java.math.BigDecimal;

/** Congelar en la línea del contrato el techo que regía el día de la firma. */
public record FreezeSubscriptionItemLimitCommand(Long companyId, Long subscriptionItemId,
        Long limitDimensionId, MeasureKind measureKind, LimitMode mode, Integer limitQuantity,
        ResetPeriod resetPeriod, LimitEnforcement enforcement, BigDecimal overageUnitAmount,
        int warnThreshold) {
}
