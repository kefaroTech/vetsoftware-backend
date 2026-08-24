package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.entitlement.domain.AccessLevel;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.entitlement.domain.EntitlementSource;
import com.vetsoftware.app.entitlement.domain.SubModuleRef;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import org.springframework.stereotype.Component;

/** El unico sitio que conoce a la vez el dominio y la fila. */
@Component
public class CompanyEntitlementJpaMapper {

    public CompanyEntitlementJpaEntity toJpa(CompanyEntitlement entitlement,
            CompanyJpaEntity company, SubModuleJpaEntity subModule) {
        CompanyEntitlementJpaEntity entity = new CompanyEntitlementJpaEntity();
        entity.setId(entitlement.getId());
        entity.setCompany(company);
        entity.setSubModule(subModule);
        entity.setAccessLevel(entitlement.getAccessLevel().name());
        entity.setSource(entitlement.getSource().name());
        entity.setSubscriptionId(entitlement.getSubscriptionId());
        entity.setSubscriptionItemId(entitlement.getSubscriptionItemId());
        entity.setValidFrom(entitlement.getValidFrom());
        entity.setValidUntil(entitlement.getValidUntil());
        entity.setRecalculatedAt(entitlement.getRecalculatedAt());
        entity.setCreatedDate(entitlement.getCreatedDate());
        return entity;
    }

    /**
     * Camino de lectura: el {@code @EntityGraph} ya hidrato el submodulo. La
     * empresa se lee por su id desde el proxy, que no dispara ninguna consulta.
     */
    public CompanyEntitlement toDomain(CompanyEntitlementJpaEntity entity) {
        SubModuleJpaEntity subModule = entity.getSubModule();
        return toDomain(entity, entity.getCompany().getId(),
                new SubModuleRef(subModule.getId(), subModule.getCode(), subModule.getName()));
    }

    /**
     * Camino de escritura: reusa la referencia que ya traia el dominio para no
     * inicializar el proxy que devolvio {@code getReferenceById}.
     */
    public CompanyEntitlement toDomain(CompanyEntitlementJpaEntity entity, Long companyId,
            SubModuleRef subModule) {
        return new CompanyEntitlement(entity.getId(), companyId, subModule,
                AccessLevel.valueOf(entity.getAccessLevel()),
                EntitlementSource.valueOf(entity.getSource()), entity.getSubscriptionId(),
                entity.getSubscriptionItemId(), entity.getValidFrom(), entity.getValidUntil(),
                entity.getRecalculatedAt(), entity.getCreatedDate());
    }
}
