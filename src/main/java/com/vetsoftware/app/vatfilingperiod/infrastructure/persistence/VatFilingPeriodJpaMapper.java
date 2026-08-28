package com.vetsoftware.app.vatfilingperiod.infrastructure.persistence;

import com.vetsoftware.app.vatfilingperiod.domain.VatFilingPeriod;
import org.springframework.stereotype.Component;

@Component
public class VatFilingPeriodJpaMapper {

    public VatFilingPeriodJpaEntity toJpa(VatFilingPeriod period) {
        VatFilingPeriodJpaEntity entity = new VatFilingPeriodJpaEntity();
        entity.setId(period.getId());
        entity.setFiscalYear((short) period.getFiscalYear());
        entity.setFrequency(period.getFrequency());
        entity.setLegalReference(period.getLegalReference());
        entity.setCreatedDate(period.getCreatedDate());
        entity.setEnabled(period.isEnabled());
        return entity;
    }

    public VatFilingPeriod toDomain(VatFilingPeriodJpaEntity entity) {
        return new VatFilingPeriod(entity.getId(), entity.getFiscalYear(), entity.getFrequency(),
                entity.getLegalReference(), entity.getCreatedDate(), entity.isEnabled());
    }
}
