package com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosticImagingTypeJpaRepository extends JpaRepository<DiagnosticImagingTypeJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<DiagnosticImagingTypeJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<DiagnosticImagingTypeJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    List<DiagnosticImagingTypeJpaEntity> findAllByGeneralTrueOrCompany_Id(Long companyId);
}
