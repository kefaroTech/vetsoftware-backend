package com.vetsoftware.app.diagnosticimaging.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosticImagingJpaRepository
    extends JpaRepository<DiagnosticImagingJpaEntity, Long> {

  @Override
  @EntityGraph(attributePaths = {"diagnosticImagingType", "animal", "consultation", "company"})
  List<DiagnosticImagingJpaEntity> findAll();

  @Override
  @EntityGraph(attributePaths = {"diagnosticImagingType", "animal", "consultation", "company"})
  Optional<DiagnosticImagingJpaEntity> findById(Long id);

  @EntityGraph(attributePaths = {"diagnosticImagingType", "animal", "consultation", "company"})
  Optional<DiagnosticImagingJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

  @EntityGraph(attributePaths = {"diagnosticImagingType", "animal", "consultation", "company"})
  List<DiagnosticImagingJpaEntity> findAllByAnimalId(Long animalId);

  @org.springframework.data.jpa.repository.Modifying(
      flushAutomatically = true,
      clearAutomatically = true)
  @org.springframework.transaction.annotation.Transactional
  @org.springframework.data.jpa.repository.Query(
      value = "UPDATE diagnostic_imagings SET enabled = true WHERE id = :id",
      nativeQuery = true)
  int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

  boolean existsByDiagnosticImagingType_Id(Long diagnosticImagingTypeId);

  boolean existsByAnimal_Id(Long animalId);

  boolean existsByConsultation_Id(Long consultationId);

  boolean existsByCompany_Id(Long companyId);
}
