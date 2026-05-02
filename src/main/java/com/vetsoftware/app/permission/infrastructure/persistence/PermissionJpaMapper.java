package com.vetsoftware.app.permission.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.permission.domain.CompanyRef;
import com.vetsoftware.app.permission.domain.Permission;
import com.vetsoftware.app.permission.domain.SubModuleRef;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PermissionJpaMapper {

    public PermissionJpaEntity toJpa(Permission permission,
                                      CompanyJpaEntity company,
                                      SubModuleJpaEntity subModule) {
        PermissionJpaEntity entity = new PermissionJpaEntity();
        entity.setId(permission.getId());
        entity.setName(permission.getName());
        entity.setCode(permission.getCode());
        entity.setCompany(company);
        entity.setSubModule(subModule);
        entity.setCreatedDate(permission.getCreatedDate());
        return entity;
    }

    public Permission toDomain(PermissionJpaEntity entity) {
        CompanyJpaEntity c = entity.getCompany();
        SubModuleJpaEntity sm = entity.getSubModule();
        return toDomain(entity,
            new CompanyRef(c.getId(), c.getName(), c.getIdentifier()),
            new SubModuleRef(sm.getId(), sm.getName(), sm.getCode()));
    }

    public Permission toDomain(PermissionJpaEntity entity, CompanyRef companyRef, SubModuleRef subModuleRef) {
        return new Permission(
            entity.getId(),
            entity.getName(),
            entity.getCode(),
            companyRef,
            subModuleRef,
            entity.getCreatedDate()
        );
    }
}
