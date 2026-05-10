package com.vetsoftware.app.employeerole.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
