package com.vetsoftware.app.permission.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.permission.domain.Permission;
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
        return new Permission(
            entity.getId(),
            entity.getName(),
            entity.getCode(),
            entity.getCompany().getId(),
            entity.getSubModule().getId(),
            entity.getCreatedDate()
        );
    }
}
