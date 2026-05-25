package com.vetsoftware.app.basepermission.infrastructure.persistence;

import com.vetsoftware.app.basepermission.application.port.out.BaseRolePermissionChildrenQueryPort;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaBaseRolePermissionChildrenQueryPort implements BaseRolePermissionChildrenQueryPort {
    private final BaseRolePermissionJpaRepository jpaRepository;

    public JpaBaseRolePermissionChildrenQueryPort(BaseRolePermissionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByBasePermissionId(Long parentId) {
        return jpaRepository.existsByBasePermission_Id(parentId);
    }
}
