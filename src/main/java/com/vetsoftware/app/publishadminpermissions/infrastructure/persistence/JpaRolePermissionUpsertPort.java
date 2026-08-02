package com.vetsoftware.app.publishadminpermissions.infrastructure.persistence;

import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaRepository;
import com.vetsoftware.app.publishadminpermissions.application.port.out.RolePermissionUpsertPort;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaEntity;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class JpaRolePermissionUpsertPort implements RolePermissionUpsertPort {

    private final RolePermissionJpaRepository rolePermissionJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;

    public JpaRolePermissionUpsertPort(RolePermissionJpaRepository rolePermissionJpaRepository,
            RoleJpaRepository roleJpaRepository, PermissionJpaRepository permissionJpaRepository) {
        this.rolePermissionJpaRepository = rolePermissionJpaRepository;
        this.roleJpaRepository = roleJpaRepository;
        this.permissionJpaRepository = permissionJpaRepository;
    }

    @Override
    public boolean linkIfAbsent(Long roleId, Long permissionId) {
        if (rolePermissionJpaRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            return false;
        }
        RoleJpaEntity roleRef = roleJpaRepository.getReferenceById(roleId);
        PermissionJpaEntity permissionRef = permissionJpaRepository.getReferenceById(permissionId);
        RolePermissionJpaEntity entity = new RolePermissionJpaEntity();
        entity.setRole(roleRef);
        entity.setPermission(permissionRef);
        entity.setCreatedDate(LocalDateTime.now());
        rolePermissionJpaRepository.save(entity);
        return true;
    }
}
