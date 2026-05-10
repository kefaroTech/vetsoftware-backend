package com.vetsoftware.app.spa.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaJpaRepository extends JpaRepository<SpaJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"spaType", "animal", "company"})
    List<SpaJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"spaType", "animal", "company"})
    Optional<SpaJpaEntity> findById(Long id);
}
