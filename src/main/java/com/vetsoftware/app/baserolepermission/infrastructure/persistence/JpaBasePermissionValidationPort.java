package com.vetsoftware.app.baserolepermission.infrastructure.persistence;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaRepository;
import com.vetsoftware.app.baserolepermission.application.port.out.BasePermissionValidationPort;
import org.springframework.stereotype.Component;

@Component("baseRolePermissionJpaBasePermissionValidationPort")
public class JpaBasePermissionValidationPort implements BasePermissionValidationPort {
    private final BasePermissionJpaRepository basePermissionJpaRepository;

    public JpaBasePermissionValidationPort(BasePermissionJpaRepository basePermissionJpaRepository) {
        this.basePermissionJpaRepository = basePermissionJpaRepository;
    }

    @Override
    public void validateExists(Long basePermissionId) {
        if (!basePermissionJpaRepository.existsById(basePermissionId)) {
            throw new IllegalArgumentException("BasePermission not found: " + basePermissionId);
        }
    }
}
