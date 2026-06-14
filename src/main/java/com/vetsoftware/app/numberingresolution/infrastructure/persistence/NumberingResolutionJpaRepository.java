package com.vetsoftware.app.numberingresolution.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NumberingResolutionJpaRepository extends JpaRepository<NumberingResolutionJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<NumberingResolutionJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<NumberingResolutionJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    List<NumberingResolutionJpaEntity> findAllByCompanyId(Long companyId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE numbering_resolutions SET enabled = true WHERE id = :id",
        nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
