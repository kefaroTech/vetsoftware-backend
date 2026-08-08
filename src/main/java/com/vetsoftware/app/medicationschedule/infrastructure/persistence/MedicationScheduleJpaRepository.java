package com.vetsoftware.app.medicationschedule.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationScheduleJpaRepository
        extends
            JpaRepository<MedicationScheduleJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"hospitalizationMedication", "createdBy"})
    Optional<MedicationScheduleJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"hospitalizationMedication", "createdBy"})
    List<MedicationScheduleJpaEntity> findByHospitalizationMedicationId(
            Long hospitalizationMedicationId);

    @EntityGraph(attributePaths = {"hospitalizationMedication", "createdBy"})
    List<MedicationScheduleJpaEntity> findByHospitalizationMedicationHospitalizationId(
            Long hospitalizationId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE medication_schedules
            SET enabled = false
            WHERE hospitalization_medication_id = :medicationId
              AND enabled = true
            """, nativeQuery = true)
    int disableByHospitalizationMedicationId(
            @org.springframework.data.repository.query.Param("medicationId") Long medicationId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE medication_schedules
            SET enabled = false
            WHERE hospitalization_medication_id = :medicationId
              AND enabled = true
              AND (applied_status IS NULL OR applied_status <> 'APPLIED')
            """, nativeQuery = true)
    int disablePendingByHospitalizationMedicationId(
            @org.springframework.data.repository.query.Param("medicationId") Long medicationId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE medication_schedules
            SET enabled = true
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
