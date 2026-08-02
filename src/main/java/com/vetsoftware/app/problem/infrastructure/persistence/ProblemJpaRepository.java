package com.vetsoftware.app.problem.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemJpaRepository extends JpaRepository<ProblemJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"animal", "company"})
    Optional<ProblemJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"animal", "company"})
    Optional<ProblemJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"animal", "company"})
    List<ProblemJpaEntity> findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(Long animalId,
            Long companyId);
}
