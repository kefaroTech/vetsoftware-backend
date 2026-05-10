package com.vetsoftware.app.testtype.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestTypeJpaRepository extends JpaRepository<TestTypeJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<TestTypeJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<TestTypeJpaEntity> findById(Long id);
}
