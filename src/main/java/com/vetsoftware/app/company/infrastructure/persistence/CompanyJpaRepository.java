package com.vetsoftware.app.company.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyJpaRepository extends JpaRepository<CompanyJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"city", "membership"})
    List<CompanyJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"city", "membership"})
    Optional<CompanyJpaEntity> findById(Long id);

    boolean existsByIdentifier(String identifier);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE companies SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
