package com.vetsoftware.app.diagnosticimaging.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DiagnosticImagingJpaRepository
        extends
            JpaRepository<DiagnosticImagingJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"diagnosticImagingType", "animal", "consultation", "company"})
    List<DiagnosticImagingJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"diagnosticImagingType", "animal", "consultation", "company"})
    Optional<DiagnosticImagingJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"diagnosticImagingType", "animal", "consultation", "company"})
    Optional<DiagnosticImagingJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"diagnosticImagingType", "animal", "consultation", "company"})
    @Query("""
            SELECT x
            FROM DiagnosticImagingJpaEntity x
            WHERE x.animal.id = :animalId
              AND x.company.id = :companyId
              AND (:q IS NULL OR :q = '' OR LOWER(x.clinicalSigns) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(x.studyType) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(x.diagnosis) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(x.observations) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<DiagnosticImagingJpaEntity> findAllByAnimalIdAndCompanyId(@Param("animalId") Long animalId,
            @Param("companyId") Long companyId, @Param("q") String q, Pageable pageable);

    boolean existsByDiagnosticImagingType_Id(Long diagnosticImagingTypeId);

    boolean existsByAnimal_Id(Long animalId);

    boolean existsByConsultation_Id(Long consultationId);

    boolean existsByCompany_Id(Long companyId);
}
