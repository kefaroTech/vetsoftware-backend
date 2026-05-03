package com.vetsoftware.app.prescription.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionJpaRepository extends JpaRepository<PrescriptionJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"animal", "company"})
    List<PrescriptionJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"animal", "company"})
    Optional<PrescriptionJpaEntity> findById(Long id);
}
