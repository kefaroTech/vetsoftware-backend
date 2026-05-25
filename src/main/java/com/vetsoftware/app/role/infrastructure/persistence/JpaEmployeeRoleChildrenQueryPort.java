package com.vetsoftware.app.role.infrastructure.persistence;

import com.vetsoftware.app.employeerole.infrastructure.persistence.EmployeeRoleJpaRepository;
import com.vetsoftware.app.role.application.port.out.EmployeeRoleChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaEmployeeRoleChildrenQueryPort implements EmployeeRoleChildrenQueryPort {
    private final EmployeeRoleJpaRepository jpaRepository;

    public JpaEmployeeRoleChildrenQueryPort(EmployeeRoleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByRoleId(Long parentId) {
        return jpaRepository.existsByRole_Id(parentId);
    }
}
