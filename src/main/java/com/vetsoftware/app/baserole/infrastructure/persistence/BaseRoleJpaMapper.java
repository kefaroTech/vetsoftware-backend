package com.vetsoftware.app.baserole.infrastructure.persistence;

import com.vetsoftware.app.baserole.domain.BaseRole;
import org.springframework.stereotype.Component;

@Component
public class BaseRoleJpaMapper {

    public BaseRoleJpaEntity toJpa(BaseRole baseRole) {
        BaseRoleJpaEntity entity = new BaseRoleJpaEntity();
        entity.setId(baseRole.getId());
        entity.setName(baseRole.getName());
        entity.setCode(baseRole.getCode());
        entity.setMandatory(baseRole.getMandatory());
        entity.setCreatedDate(baseRole.getCreatedDate());
        entity.setEnabled(baseRole.isEnabled());
        return entity;
    }

    public BaseRole toDomain(BaseRoleJpaEntity entity) {
        return new BaseRole(entity.getId(), entity.getName(), entity.getCode(),
                entity.getMandatory(), entity.getCreatedDate(), entity.isEnabled());
    }
}
