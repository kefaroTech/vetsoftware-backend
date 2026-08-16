package com.vetsoftware.app.hospitalizationobservation.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HospitalizationObservationJpaRepository
        extends
            JpaRepository<HospitalizationObservationJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"hospitalization", "createdBy"})
    Optional<HospitalizationObservationJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"hospitalization", "createdBy"})
    Optional<HospitalizationObservationJpaEntity> findByIdAndHospitalization_Company_Id(Long id,
            Long companyId);

    @EntityGraph(attributePaths = {"hospitalization", "createdBy"})
    Page<HospitalizationObservationJpaEntity> findByHospitalizationIdAndHospitalization_Company_Id(
            Long hospitalizationId, Long companyId, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE hospitalization_observations
            SET enabled = true
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
