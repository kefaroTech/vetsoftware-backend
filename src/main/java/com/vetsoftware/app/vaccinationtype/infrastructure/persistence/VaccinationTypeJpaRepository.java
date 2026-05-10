package com.vetsoftware.app.vaccinationtype.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VaccinationTypeJpaRepository extends JpaRepository<VaccinationTypeJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<VaccinationTypeJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<VaccinationTypeJpaEntity> findById(Long id);
}
