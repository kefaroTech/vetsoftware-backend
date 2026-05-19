package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaboratoryTestJpaRepository extends JpaRepository<LaboratoryTestJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"testType", "animal", "consultation", "company"})
    List<LaboratoryTestJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"testType", "animal", "consultation", "company"})
    Optional<LaboratoryTestJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"testType", "animal", "consultation", "company"})
    List<LaboratoryTestJpaEntity> findAllByAnimalId(Long animalId);
}
