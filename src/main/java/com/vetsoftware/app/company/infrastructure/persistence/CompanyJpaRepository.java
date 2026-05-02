package com.vetsoftware.app.company.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyJpaRepository extends JpaRepository<CompanyJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "city")
    List<CompanyJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "city")
    Optional<CompanyJpaEntity> findById(Long id);
}
