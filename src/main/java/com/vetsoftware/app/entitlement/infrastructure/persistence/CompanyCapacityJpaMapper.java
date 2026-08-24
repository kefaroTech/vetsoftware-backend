package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.entitlement.domain.CapacityUnit;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import org.springframework.stereotype.Component;

/** El unico sitio que conoce a la vez el contador de dominio y su fila. */
@Component
public class CompanyCapacityJpaMapper {

    public CompanyCapacityJpaEntity toJpa(CompanyCapacity capacity, CompanyJpaEntity company) {
        CompanyCapacityJpaEntity entity = new CompanyCapacityJpaEntity();
        entity.setId(capacity.getId());
        entity.setCompany(company);
        entity.setCapacityUnit(capacity.getUnit().name());
        entity.setLimitQuantity(capacity.getLimitQuantity());
        entity.setUsedQuantity(capacity.getUsedQuantity());
        entity.setSubscriptionId(capacity.getSubscriptionId());
        entity.setRecalculatedAt(capacity.getRecalculatedAt());
        entity.setCreatedDate(capacity.getCreatedDate());
        return entity;
    }

    public CompanyCapacity toDomain(CompanyCapacityJpaEntity entity) {
        return toDomain(entity, entity.getCompany().getId());
    }

    public CompanyCapacity toDomain(CompanyCapacityJpaEntity entity, Long companyId) {
        return new CompanyCapacity(entity.getId(), companyId,
                CapacityUnit.valueOf(entity.getCapacityUnit()), entity.getLimitQuantity(),
                entity.getUsedQuantity(), entity.getSubscriptionId(), entity.getRecalculatedAt(),
                entity.getCreatedDate());
    }
}
