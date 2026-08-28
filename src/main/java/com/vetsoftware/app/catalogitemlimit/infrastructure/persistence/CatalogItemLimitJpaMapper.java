package com.vetsoftware.app.catalogitemlimit.infrastructure.persistence;

import com.vetsoftware.app.catalogitemlimit.domain.CatalogItemLimit;
import com.vetsoftware.app.catalogitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.catalogitemlimit.domain.LimitMode;
import com.vetsoftware.app.catalogitemlimit.domain.MeasureKind;
import com.vetsoftware.app.catalogitemlimit.domain.ResetPeriod;
import org.springframework.stereotype.Component;

/** El único sitio que conoce a la vez el techo de fábrica y su fila. */
@Component
public class CatalogItemLimitJpaMapper {

    public CatalogItemLimitJpaEntity toJpa(CatalogItemLimit limit) {
        CatalogItemLimitJpaEntity entity = new CatalogItemLimitJpaEntity();
        entity.setId(limit.getId());
        entity.setCatalogItemId(limit.getCatalogItemId());
        entity.setLimitDimensionId(limit.getLimitDimensionId());
        entity.setMeasureKind(limit.getMeasureKind().name());
        entity.setMode(limit.getMode().name());
        entity.setLimitQuantity(limit.getLimitQuantity());
        entity.setResetPeriod(
                limit.getResetPeriod() == null ? null : limit.getResetPeriod().name());
        entity.setEnforcement(limit.getEnforcement().name());
        entity.setOverageUnitAmount(limit.getOverageUnitAmount());
        entity.setWarnThreshold((byte) limit.getWarnThreshold());
        entity.setTrialMode(limit.getTrialMode().name());
        entity.setTrialLimitQuantity(limit.getTrialLimitQuantity());
        entity.setCreatedDate(limit.getCreatedDate());
        entity.setEnabled(limit.isEnabled());
        entity.setVersion(limit.getVersion());
        return entity;
    }

    public CatalogItemLimit toDomain(CatalogItemLimitJpaEntity entity) {
        return new CatalogItemLimit(entity.getId(), entity.getCatalogItemId(),
                entity.getLimitDimensionId(), MeasureKind.valueOf(entity.getMeasureKind()),
                LimitMode.valueOf(entity.getMode()), entity.getLimitQuantity(),
                entity.getResetPeriod() == null
                        ? null
                        : ResetPeriod.valueOf(entity.getResetPeriod()),
                LimitEnforcement.valueOf(entity.getEnforcement()), entity.getOverageUnitAmount(),
                entity.getWarnThreshold(), LimitMode.valueOf(entity.getTrialMode()),
                entity.getTrialLimitQuantity(), entity.getCreatedDate(), entity.isEnabled(),
                entity.getVersion());
    }
}
