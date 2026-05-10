package com.vetsoftware.app.employee.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeJpaRepository extends JpaRepository<EmployeeJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<EmployeeJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<EmployeeJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    @Query("SELECT e FROM EmployeeJpaEntity e WHERE e.company.id = :companyId")
    List<EmployeeJpaEntity> findAllByCompanyId(@Param("companyId") Long companyId);

    boolean existsByEmployeeCode(String employeeCode);

    @EntityGraph(attributePaths = "company")
    Optional<EmployeeJpaEntity> findByEmployeeCodeAndStatus(String employeeCode, String status);
}
