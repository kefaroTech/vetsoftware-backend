package com.vetsoftware.app.specie.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecieJpaRepository extends JpaRepository<SpecieJpaEntity, Long> {

  @org.springframework.data.jpa.repository.Modifying(
      flushAutomatically = true,
      clearAutomatically = true)
  @org.springframework.transaction.annotation.Transactional
  @org.springframework.data.jpa.repository.Query(
      value = "UPDATE species SET enabled = true WHERE id = :id",
      nativeQuery = true)
  int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
