package com.vetsoftware.app.employeerole.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRoleJpaRepository extends JpaRepository<EmployeeRoleJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"employee", "role"})
    List<EmployeeRoleJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"employee", "role"})
    Optional<EmployeeRoleJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "role")
    List<EmployeeRoleJpaEntity> findByEmployeeId(Long employeeId);

    @EntityGraph(attributePaths = "role")
    List<EmployeeRoleJpaEntity> findByEmployeeIdIn(List<Long> employeeIds);

    // Roles para la pantalla de listado de empleados: los asignados vigentes
    // (employee_role activo)
    // para
    // cualquiera, MÁS todos los del empleado desactivado aunque su employee_role
    // esté deshabilitado
    // (así un
    // inactivo muestra "el rol que tenía"). Nativa para saltar los @SQLRestriction
    // de ambas tablas.
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT er.*
            FROM employee_roles er
            JOIN employees e ON e.id = er.employee_id
            WHERE er.employee_id IN :employeeIds
              AND (er.enabled = true OR e.enabled = false)
            """, nativeQuery = true)
    List<EmployeeRoleJpaEntity> findForEmployeeListing(
            @org.springframework.data.repository.query.Param("employeeIds") List<Long> employeeIds);

    @Query("""
            select er.employee.id from EmployeeRoleJpaEntity er where er.role.id = :roleId
            """)
    List<Long> findEmployeeIdsByRoleId(Long roleId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE employee_roles
            SET enabled = true
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT id
            FROM employee_roles
            WHERE employee_id = :employeeId
              AND role_id = :roleId
              AND enabled = false
            LIMIT 1
            """, nativeQuery = true)
    java.util.Optional<Long> findDisabledIdByEmployeeAndRole(
            @org.springframework.data.repository.query.Param("employeeId") Long employeeId,
            @org.springframework.data.repository.query.Param("roleId") Long roleId);

    boolean existsByEmployee_Id(Long employeeId);

    boolean existsByRole_Id(Long roleId);
}
