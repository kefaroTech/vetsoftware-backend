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

    @Query("select er.employee.id from EmployeeRoleJpaEntity er where er.role.id = :roleId")
    List<Long> findEmployeeIdsByRoleId(Long roleId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE employee_roles SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByEmployee_Id(Long employeeId);

    boolean existsByRole_Id(Long roleId);
}
