package com.vetsoftware.app.diagnosticimaging.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosticImagingJpaRepository extends JpaRepository<DiagnosticImagingJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"diagnosticImagingType", "animal", "consultation", "company"})
    List<DiagnosticImagingJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"diagnosticImagingType", "animal", "consultation", "company"})
    Optional<DiagnosticImagingJpaEntity> findById(Long id);
}
