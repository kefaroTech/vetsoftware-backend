package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.company.domain.CityRef;
import com.vetsoftware.app.company.domain.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyJpaMapper {

    public CompanyJpaEntity toJpa(Company company, CityJpaEntity city) {
        CompanyJpaEntity entity = new CompanyJpaEntity();
        entity.setId(company.getId());
        entity.setName(company.getName());
        entity.setIdentifier(company.getIdentifier());
        entity.setAddress(company.getAddress());
        entity.setContactNumber(company.getContactNumber());
        entity.setCity(city);
        entity.setCreatedDate(company.getCreatedDate());
        entity.setVersion(company.getVersion());
        entity.setEnabled(company.isEnabled());
        return entity;
    }

    public Company toDomain(CompanyJpaEntity entity) {
        CityJpaEntity c = entity.getCity();
        return toDomain(entity, new CityRef(c.getId(), c.getName()));
    }

    public Company toDomain(CompanyJpaEntity entity, CityRef cityRef) {
        return new Company(entity.getId(), entity.getName(), entity.getIdentifier(),
                entity.getAddress(), entity.getContactNumber(), cityRef, entity.getCreatedDate(),
                entity.getVersion(), entity.isEnabled());
    }
}
