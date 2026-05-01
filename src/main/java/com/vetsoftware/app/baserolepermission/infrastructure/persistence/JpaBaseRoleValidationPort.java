package com.vetsoftware.app.baserolepermission.infrastructure.persistence;

import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaRepository;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRoleValidationPort;
import org.springframework.stereotype.Component;

@Component("baseRolePermissionJpaBaseRoleValidationPort")
public class JpaBaseRoleValidationPort implements BaseRoleValidationPort {
    private final BaseRoleJpaRepository baseRoleJpaRepository;

    public JpaBaseRoleValidationPort(BaseRoleJpaRepository baseRoleJpaRepository) {
        this.baseRoleJpaRepository = baseRoleJpaRepository;
    }

    @Override
    public void validateExists(Long baseRoleId) {
        if (!baseRoleJpaRepository.existsById(baseRoleId)) {
            throw new IllegalArgumentException("BaseRole not found: " + baseRoleId);
        }
    }
}
