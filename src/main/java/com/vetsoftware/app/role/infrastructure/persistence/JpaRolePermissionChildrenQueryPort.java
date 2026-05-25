package com.vetsoftware.app.role.infrastructure.persistence;

import com.vetsoftware.app.role.application.port.out.RolePermissionChildrenQueryPort;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaRolePermissionChildrenQueryPort implements RolePermissionChildrenQueryPort {
    private final RolePermissionJpaRepository jpaRepository;

    public JpaRolePermissionChildrenQueryPort(RolePermissionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByRoleId(Long parentId) {
        return jpaRepository.existsByRole_Id(parentId);
    }
}
