package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.company.domain.CityRef;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.company.domain.MembershipRef;
import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CompanyJpaMapper {

    public CompanyJpaEntity toJpa(Company company, CityJpaEntity city, MembershipJpaEntity membership) {
        CompanyJpaEntity entity = new CompanyJpaEntity();
        entity.setId(company.getId());
        entity.setName(company.getName());
        entity.setIdentifier(company.getIdentifier());
        entity.setAddress(company.getAddress());
        entity.setContactNumber(company.getContactNumber());
        entity.setCity(city);
        entity.setMembership(membership);
        entity.setCreatedDate(company.getCreatedDate());
        return entity;
    }

    public Company toDomain(CompanyJpaEntity entity) {
        CityJpaEntity c = entity.getCity();
        MembershipJpaEntity m = entity.getMembership();
        return toDomain(entity,
            new CityRef(c.getId(), c.getName()),
            new MembershipRef(m.getId(), m.getName(), m.getStatus()));
    }

    public Company toDomain(CompanyJpaEntity entity, CityRef cityRef, MembershipRef membershipRef) {
        return new Company(
            entity.getId(),
            entity.getName(),
            entity.getIdentifier(),
            entity.getAddress(),
            entity.getContactNumber(),
            cityRef,
            membershipRef,
            entity.getCreatedDate()
        );
    }
}
