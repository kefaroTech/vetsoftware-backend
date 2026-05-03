package com.vetsoftware.app.owner.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerJpaRepository extends JpaRepository<OwnerJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"city", "company"})
    List<OwnerJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"city", "company"})
    Optional<OwnerJpaEntity> findById(Long id);
}
