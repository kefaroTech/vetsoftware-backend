package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.employeerole.infrastructure.persistence.EmployeeRoleJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRolePermissionJpaRepository
        extends JpaRepository<EmployeeRoleJpaEntity, Long> {

    @Query("""
            SELECT rp.permission.code
            FROM EmployeeRoleJpaEntity er
            JOIN RolePermissionJpaEntity rp ON rp.role = er.role
            WHERE er.employee.id = :employeeId
            """)
    List<String> findPermissionCodesByEmployeeId(@Param("employeeId") Long employeeId);
}
