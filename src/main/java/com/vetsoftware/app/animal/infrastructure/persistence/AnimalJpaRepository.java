package com.vetsoftware.app.animal.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalJpaRepository extends JpaRepository<AnimalJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"specie", "breed", "owner", "company", "color"})
    List<AnimalJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"specie", "breed", "owner", "company", "color"})
    Optional<AnimalJpaEntity> findById(Long id);
}
