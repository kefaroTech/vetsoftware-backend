package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalizationProgressNoteJpaRepository
    extends JpaRepository<HospitalizationProgressNoteJpaEntity, Long> {

  @Override
  @EntityGraph(attributePaths = {"hospitalization", "createdBy"})
  Optional<HospitalizationProgressNoteJpaEntity> findById(Long id);

  @EntityGraph(attributePaths = {"hospitalization", "createdBy"})
  Optional<HospitalizationProgressNoteJpaEntity> findByIdAndHospitalization_Company_Id(
      Long id, Long companyId);

  @EntityGraph(attributePaths = {"hospitalization", "createdBy"})
  List<HospitalizationProgressNoteJpaEntity> findByHospitalizationId(Long hospitalizationId);

  @org.springframework.data.jpa.repository.Modifying(
      flushAutomatically = true,
      clearAutomatically = true)
  @org.springframework.transaction.annotation.Transactional
  @org.springframework.data.jpa.repository.Query(
      value = "UPDATE hospitalization_progress_notes SET enabled = true WHERE id = :id",
      nativeQuery = true)
  int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
