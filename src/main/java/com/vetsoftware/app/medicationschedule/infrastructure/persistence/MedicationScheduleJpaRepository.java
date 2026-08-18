package com.vetsoftware.app.medicationschedule.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MedicationScheduleJpaRepository
        extends
            JpaRepository<MedicationScheduleJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"hospitalizationMedication", "createdBy"})
    Optional<MedicationScheduleJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"hospitalizationMedication", "createdBy"})
    List<MedicationScheduleJpaEntity> findByHospitalizationMedicationId(
            Long hospitalizationMedicationId);

    /**
     * La toma no tiene {@code company_id} propio: la empresa vive dos saltos arriba
     * ({@code hospitalization_medications} → {@code hospitalizations}). Acotar por
     * {@code hospitalization_medication_id} a secas NO prueba nada —es una FK
     * ajena, el paciente es de alguien—, así que el filtro sube por la asociación
     * hasta {@code hospitalizations.company_id}.
     */
    @EntityGraph(attributePaths = {"hospitalizationMedication", "createdBy"})
    @Query("""
            SELECT s
            FROM MedicationScheduleJpaEntity s
            WHERE s.hospitalizationMedication.id = :medicationId
              AND s.hospitalizationMedication.hospitalization.company.id = :companyId
            """)
    List<MedicationScheduleJpaEntity> findByHospitalizationMedicationIdAndCompanyId(
            @Param("medicationId") Long medicationId, @Param("companyId") Long companyId);

    @EntityGraph(attributePaths = {"hospitalizationMedication", "createdBy"})
    List<MedicationScheduleJpaEntity> findByHospitalizationMedicationHospitalizationId(
            Long hospitalizationId);

    /**
     * Mismo camino a la empresa que
     * {@link #findByHospitalizationMedicationIdAndCompanyId}.
     */
    @EntityGraph(attributePaths = {"hospitalizationMedication", "createdBy"})
    @Query("""
            SELECT s
            FROM MedicationScheduleJpaEntity s
            WHERE s.hospitalizationMedication.hospitalization.id = :hospitalizationId
              AND s.hospitalizationMedication.hospitalization.company.id = :companyId
            """)
    List<MedicationScheduleJpaEntity> findByHospitalizationIdAndCompanyId(
            @Param("hospitalizationId") Long hospitalizationId, @Param("companyId") Long companyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE medication_schedules
            SET enabled = false
            WHERE hospitalization_medication_id = :medicationId
              AND enabled = true
            """, nativeQuery = true)
    int disableByHospitalizationMedicationId(@Param("medicationId") Long medicationId);

    /**
     * El filtro por empresa no es defensa en profundidad: es LA defensa. Este
     * UPDATE borra la hoja de medicación entera de una orden, y sin él bastaba
     * adivinar el {@code hospitalization_medication_id} de otro tenant para dejarle
     * al paciente ajeno el plan de tomas en blanco.
     *
     * <p>
     * La empresa no cuelga de {@code medication_schedules}: cuelga de la
     * hospitalización, dos saltos arriba, así que el filtro viaja por un
     * {@code EXISTS} contra {@code hospitalization_medications} unido a
     * {@code hospitalizations} — la misma ruta que usa
     * {@code findByHospitalizationMedicationIdAndCompanyId}.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE medication_schedules s
            SET s.enabled = false
            WHERE s.hospitalization_medication_id = :medicationId
              AND s.enabled = true
              AND EXISTS (SELECT 1
                          FROM hospitalization_medications m
                          JOIN hospitalizations h ON h.id = m.hospitalization_id
                          WHERE m.id = s.hospitalization_medication_id
                            AND h.company_id = :companyId)
            """, nativeQuery = true)
    int disableByHospitalizationMedicationId(@Param("medicationId") Long medicationId,
            @Param("companyId") Long companyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE medication_schedules
            SET enabled = false
            WHERE hospitalization_medication_id = :medicationId
              AND enabled = true
              AND (applied_status IS NULL OR applied_status <> 'APPLIED')
            """, nativeQuery = true)
    int disablePendingByHospitalizationMedicationId(@Param("medicationId") Long medicationId);

    /**
     * Suspensión acotada al tenant, y aquí el {@code WHERE} es toda la seguridad
     * que hay: {@code SuspendPendingMedicationSchedules} no lee la toma antes de
     * escribir —decide qué devolver mirando lo que quedó vivo—, así que sin este
     * filtro cualquiera con {@code hospitalization.update} suspendía las tomas
     * pendientes de un paciente de otro tenant. Mismo {@code EXISTS} que
     * {@link #disableByHospitalizationMedicationId(Long, Long)}.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE medication_schedules s
            SET s.enabled = false
            WHERE s.hospitalization_medication_id = :medicationId
              AND s.enabled = true
              AND (s.applied_status IS NULL OR s.applied_status <> 'APPLIED')
              AND EXISTS (SELECT 1
                          FROM hospitalization_medications m
                          JOIN hospitalizations h ON h.id = m.hospitalization_id
                          WHERE m.id = s.hospitalization_medication_id
                            AND h.company_id = :companyId)
            """, nativeQuery = true)
    int disablePendingByHospitalizationMedicationId(@Param("medicationId") Long medicationId,
            @Param("companyId") Long companyId);
}
