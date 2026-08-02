package com.vetsoftware.app.systemuser.infrastructure.persistence;

import com.vetsoftware.app.systemuser.domain.SystemUser;
import org.springframework.stereotype.Component;

@Component
public class SystemUserJpaMapper {

    public SystemUserJpaEntity toJpa(SystemUser systemUser) {
        SystemUserJpaEntity entity = new SystemUserJpaEntity();
        entity.setId(systemUser.getId());
        entity.setCode(systemUser.getCode());
        entity.setHashPassword(systemUser.getHashPassword());
        entity.setCreatedDate(systemUser.getCreatedDate());
        entity.setEnabled(systemUser.isEnabled());
        entity.setAuthVersion(systemUser.getAuthVersion());
        return entity;
    }

    public SystemUser toDomain(SystemUserJpaEntity entity) {
        return new SystemUser(entity.getId(), entity.getCode(), entity.getHashPassword(),
                entity.getCreatedDate(), entity.isEnabled(), entity.getAuthVersion());
    }
}
