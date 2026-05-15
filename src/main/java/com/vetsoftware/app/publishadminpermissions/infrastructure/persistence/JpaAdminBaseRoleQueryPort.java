package com.vetsoftware.app.publishadminpermissions.infrastructure.persistence;

import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaEntity;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaRepository;
import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBaseRoleQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaAdminBaseRoleQueryPort implements AdminBaseRoleQueryPort {

    private static final String ADMIN_CODE = "ADMIN";

    private final BaseRoleJpaRepository baseRoleJpaRepository;

    public JpaAdminBaseRoleQueryPort(BaseRoleJpaRepository baseRoleJpaRepository) {
        this.baseRoleJpaRepository = baseRoleJpaRepository;
    }

    @Override
    public Optional<Long> findAdminBaseRoleId() {
        return baseRoleJpaRepository.findByCode(ADMIN_CODE).map(BaseRoleJpaEntity::getId);
    }
}
