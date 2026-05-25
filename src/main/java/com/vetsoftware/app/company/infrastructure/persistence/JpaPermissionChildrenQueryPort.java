package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.application.port.out.PermissionChildrenQueryPort;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaPermissionChildrenQueryPort implements PermissionChildrenQueryPort {
    private final PermissionJpaRepository jpaRepository;

    public JpaPermissionChildrenQueryPort(PermissionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByCompanyId(Long parentId) {
        return jpaRepository.existsByCompany_Id(parentId);
    }
}
