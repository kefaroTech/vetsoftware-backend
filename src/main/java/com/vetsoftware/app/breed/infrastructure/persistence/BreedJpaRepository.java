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

  @org.springframework.data.jpa.repository.Modifying(
      flushAutomatically = true,
      clearAutomatically = true)
  @org.springframework.transaction.annotation.Transactional
  @org.springframework.data.jpa.repository.Query(
      value = "UPDATE breeds SET enabled = true WHERE id = :id",
      nativeQuery = true)
  int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

  boolean existsBySpecie_Id(Long specieId);
}
