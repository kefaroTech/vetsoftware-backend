package com.vetsoftware.app.subscriptionitemlimit.infrastructure.persistence;

import com.vetsoftware.app.subscriptionitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.subscriptionitemlimit.domain.LimitMode;
import com.vetsoftware.app.subscriptionitemlimit.domain.MeasureKind;
import com.vetsoftware.app.subscriptionitemlimit.domain.ResetPeriod;
import com.vetsoftware.app.subscriptionitemlimit.domain.SubscriptionItemLimit;
import org.springframework.stereotype.Component;

/** El único sitio que conoce a la vez el techo congelado y su fila. */
@Component
public class SubscriptionItemLimitJpaMapper {

    public SubscriptionItemLimitJpaEntity toJpa(SubscriptionItemLimit limit) {
        SubscriptionItemLimitJpaEntity entity = new SubscriptionItemLimitJpaEntity();
        entity.setId(limit.getId());
        entity.setCompanyId(limit.getCompanyId());
        entity.setSubscriptionItemId(limit.getSubscriptionItemId());
        entity.setLimitDimensionId(limit.getLimitDimensionId());
        entity.setMeasureKind(limit.getMeasureKind().name());
        entity.setMode(limit.getMode().name());
        entity.setLimitQuantity(limit.getLimitQuantity());
        entity.setResetPeriod(
                limit.getResetPeriod() == null ? null : limit.getResetPeriod().name());
        entity.setEnforcement(limit.getEnforcement().name());
        entity.setOverageUnitAmount(limit.getOverageUnitAmount());
        entity.setWarnThreshold((byte) limit.getWarnThreshold());
        entity.setCreatedDate(limit.getCreatedDate());
        entity.setEnabled(limit.isEnabled());
        entity.setVersion(limit.getVersion());
        return entity;
    }

    public SubscriptionItemLimit toDomain(SubscriptionItemLimitJpaEntity entity) {
        return new SubscriptionItemLimit(entity.getId(), entity.getCompanyId(),
                entity.getSubscriptionItemId(), entity.getLimitDimensionId(),
                MeasureKind.valueOf(entity.getMeasureKind()), LimitMode.valueOf(entity.getMode()),
                entity.getLimitQuantity(),
                entity.getResetPeriod() == null
                        ? null
                        : ResetPeriod.valueOf(entity.getResetPeriod()),
                LimitEnforcement.valueOf(entity.getEnforcement()), entity.getOverageUnitAmount(),
                entity.getWarnThreshold(), entity.getCreatedDate(), entity.isEnabled(),
                entity.getVersion());
    }
}
