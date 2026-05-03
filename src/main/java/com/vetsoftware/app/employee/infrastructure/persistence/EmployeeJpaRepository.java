package com.vetsoftware.app.employee.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeJpaRepository extends JpaRepository<EmployeeJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<EmployeeJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<EmployeeJpaEntity> findById(Long id);

    boolean existsByEmployeeCode(String employeeCode);

    @EntityGraph(attributePaths = "company")
    Optional<EmployeeJpaEntity> findByEmployeeCodeAndStatus(String employeeCode, String status);
}
