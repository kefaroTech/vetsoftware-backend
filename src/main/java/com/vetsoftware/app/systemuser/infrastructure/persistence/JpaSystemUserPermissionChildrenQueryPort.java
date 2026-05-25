package com.vetsoftware.app.systemuser.infrastructure.persistence;

import com.vetsoftware.app.systemuser.application.port.out.SystemUserPermissionChildrenQueryPort;
import com.vetsoftware.app.systemuserpermission.infrastructure.persistence.SystemUserPermissionJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaSystemUserPermissionChildrenQueryPort implements SystemUserPermissionChildrenQueryPort {
    private final SystemUserPermissionJpaRepository jpaRepository;

    public JpaSystemUserPermissionChildrenQueryPort(SystemUserPermissionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveBySystemUserId(Long parentId) {
        return jpaRepository.existsBySystemUser_Id(parentId);
    }
}
