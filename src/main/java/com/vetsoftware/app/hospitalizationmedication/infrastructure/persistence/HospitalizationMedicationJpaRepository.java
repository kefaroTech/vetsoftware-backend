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
    Page<HospitalizationMedicationJpaEntity> findByHospitalizationId(Long hospitalizationId,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE hospitalization_medications
            SET enabled = true
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
