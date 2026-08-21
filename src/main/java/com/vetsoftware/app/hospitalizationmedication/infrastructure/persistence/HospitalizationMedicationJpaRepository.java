package com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HospitalizationMedicationJpaRepository
        extends
            JpaRepository<HospitalizationMedicationJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"hospitalization", "createdBy", "suspensionBy"})
    Optional<HospitalizationMedicationJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"hospitalization", "createdBy", "suspensionBy"})
    Optional<HospitalizationMedicationJpaEntity> findByIdAndHospitalization_Company_Id(Long id,
            Long companyId);

    @EntityGraph(attributePaths = {"hospitalization", "createdBy", "suspensionBy"})
    Page<HospitalizationMedicationJpaEntity> findByHospitalizationIdAndHospitalization_Company_Id(
            Long hospitalizationId, Long companyId, Pageable pageable);
}
