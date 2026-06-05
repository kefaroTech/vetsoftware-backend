package com.vetsoftware.app.service.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.service.domain.CompanyRef;
import com.vetsoftware.app.service.domain.Service;
import com.vetsoftware.app.service.domain.ServiceCategoryRef;
import com.vetsoftware.app.service.domain.TaxRef;
import com.vetsoftware.app.servicecategory.infrastructure.persistence.ServiceCategoryJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ServiceJpaMapper {

    public ServiceJpaEntity toJpa(Service service,
                                  ServiceCategoryJpaEntity serviceCategory,
                                  TaxJpaEntity tax,
                                  CompanyJpaEntity company) {
        ServiceJpaEntity entity = new ServiceJpaEntity();
        entity.setId(service.getId());
        entity.setName(service.getName());
        entity.setPrice(service.getPrice());
        entity.setHasTax(service.isHasTax());
        entity.setNotes(service.getNotes());
        entity.setServiceCategory(serviceCategory);
        entity.setTax(tax);
        entity.setCompany(company);
        entity.setCreatedDate(service.getCreatedDate());
        entity.setEnabled(service.isEnabled());
        return entity;
    }

    public Service toDomain(ServiceJpaEntity entity) {
        ServiceCategoryJpaEntity sc = entity.getServiceCategory();
        TaxJpaEntity t = entity.getTax();
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
            new ServiceCategoryRef(sc.getId(), sc.getName()),
            t == null ? null : new TaxRef(t.getId(), t.getName(), t.getPercentage()),
            new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public Service toDomain(ServiceJpaEntity entity, ServiceCategoryRef serviceCategoryRef,
                            TaxRef taxRef, CompanyRef companyRef) {
        return new Service(
            entity.getId(),
            entity.getName(),
            entity.getPrice(),
            entity.isHasTax(),
            entity.getNotes(),
            serviceCategoryRef,
            taxRef,
            companyRef,
            entity.getCreatedDate(),
            entity.isEnabled());
    }
}
