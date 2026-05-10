package com.vetsoftware.app.vaccination.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VaccinationJpaRepository extends JpaRepository<VaccinationJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"vaccinationType", "animal", "consultation", "company"})
    List<VaccinationJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"vaccinationType", "animal", "consultation", "company"})
    Optional<VaccinationJpaEntity> findById(Long id);
}
