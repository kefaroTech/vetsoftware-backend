package com.vetsoftware.app.subscriptionitemlimit.application.dto;

import com.vetsoftware.app.subscriptionitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.subscriptionitemlimit.domain.LimitMode;
import com.vetsoftware.app.subscriptionitemlimit.domain.MeasureKind;
import com.vetsoftware.app.subscriptionitemlimit.domain.ResetPeriod;
import com.vetsoftware.app.subscriptionitemlimit.domain.SubscriptionItemLimit;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** El techo congelado tal como sale de la feature. */
public record SubscriptionItemLimitDto(Long id, Long companyId, Long subscriptionItemId,
        Long limitDimensionId, MeasureKind measureKind, LimitMode mode, Integer limitQuantity,
        ResetPeriod resetPeriod, LimitEnforcement enforcement, BigDecimal overageUnitAmount,
        int warnThreshold, LocalDateTime createdDate) {

    public static SubscriptionItemLimitDto from(SubscriptionItemLimit limit) {
        return new SubscriptionItemLimitDto(limit.getId(), limit.getCompanyId(),
                limit.getSubscriptionItemId(), limit.getLimitDimensionId(), limit.getMeasureKind(),
                limit.getMode(), limit.getLimitQuantity(), limit.getResetPeriod(),
                limit.getEnforcement(), limit.getOverageUnitAmount(), limit.getWarnThreshold(),
                limit.getCreatedDate());
    }
}
