package com.vetsoftware.app.problem.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProblemJpaRepository extends JpaRepository<ProblemJpaEntity, Long> {

    @EntityGraph(attributePaths = {"animal", "company"})
    Page<ProblemJpaEntity> findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(Long animalId,
            Long companyId, Pageable pageable);
}
