package com.vetsoftware.app.submodule.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubModuleJpaRepository extends JpaRepository<SubModuleJpaEntity, Long> {

  @Override
  @EntityGraph(attributePaths = "module")
  List<SubModuleJpaEntity> findAll();

  @Override
  @EntityGraph(attributePaths = "module")
  Optional<SubModuleJpaEntity> findById(Long id);

  @org.springframework.data.jpa.repository.Modifying(
      flushAutomatically = true,
      clearAutomatically = true)
  @org.springframework.transaction.annotation.Transactional
  @org.springframework.data.jpa.repository.Query(
      value = "UPDATE sub_modules SET enabled = true WHERE id = :id",
      nativeQuery = true)
  int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

  boolean existsByModule_Id(Long moduleId);
}
