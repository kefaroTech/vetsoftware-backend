package com.vetsoftware.app.animalalert.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalAlertJpaRepository extends JpaRepository<AnimalAlertJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"animal", "company"})
    Optional<AnimalAlertJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"animal", "company"})
    List<AnimalAlertJpaEntity> findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(Long animalId,
            Long companyId);
}
