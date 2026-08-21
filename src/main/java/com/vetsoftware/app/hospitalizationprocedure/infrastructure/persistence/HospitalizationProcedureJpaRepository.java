package com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HospitalizationProcedureJpaRepository
        extends
            JpaRepository<HospitalizationProcedureJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"hospitalization", "createdBy", "suspensionBy"})
    Optional<HospitalizationProcedureJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"hospitalization", "createdBy", "suspensionBy"})
    Optional<HospitalizationProcedureJpaEntity> findByIdAndHospitalization_Company_Id(Long id,
            Long companyId);

    @EntityGraph(attributePaths = {"hospitalization", "createdBy", "suspensionBy"})
    Page<HospitalizationProcedureJpaEntity> findByHospitalizationIdAndHospitalization_Company_Id(
            Long hospitalizationId, Long companyId, Pageable pageable);
}
