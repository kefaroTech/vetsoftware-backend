package com.vetsoftware.app.role.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.role.domain.CompanyRef;
import com.vetsoftware.app.role.domain.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleJpaMapper {

    public RoleJpaEntity toJpa(Role role, CompanyJpaEntity company) {
        RoleJpaEntity entity = new RoleJpaEntity();
        entity.setId(role.getId());
        entity.setName(role.getName());
        entity.setCode(role.getCode());
        entity.setCompany(company);
        entity.setCreatedDate(role.getCreatedDate());
        return entity;
    }

    public Role toDomain(RoleJpaEntity entity) {
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity, new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public Role toDomain(RoleJpaEntity entity, CompanyRef companyRef) {
        return new Role(
            entity.getId(),
            entity.getName(),
            entity.getCode(),
            companyRef,
            entity.getCreatedDate()
        );
    }
}
