package com.vetsoftware.app.city.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityJpaRepository extends JpaRepository<CityJpaEntity, Long> {

  @Override
  @EntityGraph(attributePaths = "state")
  List<CityJpaEntity> findAll();

  @Override
  @EntityGraph(attributePaths = "state")
  Optional<CityJpaEntity> findById(Long id);

  @EntityGraph(attributePaths = "state")
  List<CityJpaEntity> findAllByState_Id(Long stateId);

  @org.springframework.data.jpa.repository.Modifying(
      flushAutomatically = true,
      clearAutomatically = true)
  @org.springframework.transaction.annotation.Transactional
  @org.springframework.data.jpa.repository.Query(
      value = "UPDATE cities SET enabled = true WHERE id = :id",
      nativeQuery = true)
  int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

  boolean existsByState_Id(Long stateId);
}
