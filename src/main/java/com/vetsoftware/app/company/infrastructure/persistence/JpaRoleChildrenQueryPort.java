package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.application.port.out.RoleChildrenQueryPort;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaRoleChildrenQueryPort implements RoleChildrenQueryPort {
    private final RoleJpaRepository jpaRepository;

    public JpaRoleChildrenQueryPort(RoleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByCompanyId(Long parentId) {
        return jpaRepository.existsByCompany_Id(parentId);
    }
}
