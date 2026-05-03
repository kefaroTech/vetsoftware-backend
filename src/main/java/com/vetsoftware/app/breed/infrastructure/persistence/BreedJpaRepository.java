package com.vetsoftware.app.breed.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BreedJpaRepository extends JpaRepository<BreedJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "specie")
    List<BreedJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "specie")
    Optional<BreedJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "specie")
    List<BreedJpaEntity> findAllBySpecie_Id(Long specieId);
}
