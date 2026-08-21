package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HospitalizationProgressNoteJpaRepository
        extends
            JpaRepository<HospitalizationProgressNoteJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"hospitalization", "createdBy"})
    Optional<HospitalizationProgressNoteJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"hospitalization", "createdBy"})
    Optional<HospitalizationProgressNoteJpaEntity> findByIdAndHospitalization_Company_Id(Long id,
            Long companyId);

    @EntityGraph(attributePaths = {"hospitalization", "createdBy"})
    Page<HospitalizationProgressNoteJpaEntity> findByHospitalizationIdAndHospitalization_Company_Id(
            Long hospitalizationId, Long companyId, Pageable pageable);
}
