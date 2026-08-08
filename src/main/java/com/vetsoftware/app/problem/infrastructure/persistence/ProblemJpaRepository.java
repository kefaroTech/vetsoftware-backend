package com.vetsoftware.app.problem.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProblemJpaRepository extends JpaRepository<ProblemJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"animal", "company"})
    Optional<ProblemJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"animal", "company"})
    Optional<ProblemJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"animal", "company"})
    Page<ProblemJpaEntity> findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(Long animalId,
            Long companyId, Pageable pageable);
}
