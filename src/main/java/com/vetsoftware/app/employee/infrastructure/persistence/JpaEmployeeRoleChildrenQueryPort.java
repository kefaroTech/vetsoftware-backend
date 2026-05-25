package com.vetsoftware.app.employee.infrastructure.persistence;

import com.vetsoftware.app.employee.application.port.out.EmployeeRoleChildrenQueryPort;
import com.vetsoftware.app.employeerole.infrastructure.persistence.EmployeeRoleJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaEmployeeRoleChildrenQueryPort implements EmployeeRoleChildrenQueryPort {
    private final EmployeeRoleJpaRepository jpaRepository;

    public JpaEmployeeRoleChildrenQueryPort(EmployeeRoleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByEmployeeId(Long parentId) {
        return jpaRepository.existsByEmployee_Id(parentId);
    }
}
