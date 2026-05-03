package com.vetsoftware.app.deworming.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DewormingJpaRepository extends JpaRepository<DewormingJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"animal", "company"})
    List<DewormingJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"animal", "company"})
    Optional<DewormingJpaEntity> findById(Long id);
}
