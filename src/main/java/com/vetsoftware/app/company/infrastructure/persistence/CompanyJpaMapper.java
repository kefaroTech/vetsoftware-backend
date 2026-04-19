package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.domain.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyJpaMapper {
    public CompanyJpaEntity toJpa(Company company) {
        CompanyJpaEntity entity = new CompanyJpaEntity();
        entity.setId(company.getId());
        entity.setName(company.getName());
        entity.setIdentifier(company.getIdentifier());
        entity.setAddress(company.getAddress());
        entity.setContactNumber(company.getContactNumber());
        entity.setCreatedDate(company.getCreatedDate());
        entity.setCreatedBy(company.getCreatedBy());
        return entity;
    }

    public Company toDomain(CompanyJpaEntity entity) {
        return new Company(
            entity.getId(),
            entity.getName(),
            entity.getIdentifier(),
            entity.getAddress(),
            entity.getContactNumber(),
            entity.getCreatedDate(),
            entity.getCreatedBy()
        );
    }
}
