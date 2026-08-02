package com.vetsoftware.app.servicecategory.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.servicecategory.domain.CompanyRef;
import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import org.springframework.stereotype.Component;

@Component
public class ServiceCategoryJpaMapper {
    public ServiceCategoryJpaEntity toJpa(ServiceCategory serviceCategory,
            CompanyJpaEntity company) {
        ServiceCategoryJpaEntity entity = new ServiceCategoryJpaEntity();
        entity.setId(serviceCategory.getId());
        entity.setName(serviceCategory.getName());
        entity.setDescription(serviceCategory.getDescription());
        entity.setCompany(company);
        entity.setCreatedDate(serviceCategory.getCreatedDate());
        entity.setUpdatedDate(serviceCategory.getUpdatedDate());
        entity.setUpdatedBy(serviceCategory.getUpdatedBy());
        entity.setVersion(serviceCategory.getVersion());
        entity.setEnabled(serviceCategory.isEnabled());
        return entity;
    }

    public ServiceCategory toDomain(ServiceCategoryJpaEntity entity) {
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity, new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public ServiceCategory toDomain(ServiceCategoryJpaEntity entity, CompanyRef companyRef) {
        return new ServiceCategory(entity.getId(), entity.getName(), entity.getDescription(),
                companyRef, entity.getCreatedDate(), entity.getUpdatedDate(), entity.getUpdatedBy(),
                entity.getVersion(), entity.isEnabled());
    }
}
