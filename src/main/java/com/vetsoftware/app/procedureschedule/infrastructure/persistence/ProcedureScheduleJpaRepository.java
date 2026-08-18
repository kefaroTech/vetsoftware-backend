package com.vetsoftware.app.procedureschedule.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ProcedureScheduleJpaRepository
        extends
            JpaRepository<ProcedureScheduleJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"hospitalizationProcedure", "createdBy"})
    Optional<ProcedureScheduleJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"hospitalizationProcedure", "createdBy"})
    List<ProcedureScheduleJpaEntity> findByHospitalizationProcedureId(
            Long hospitalizationProcedureId);

    /**
     * La ejecución no tiene {@code company_id} propio: la empresa vive dos saltos
     * arriba ({@code hospitalization_procedures} → {@code hospitalizations}).
     * Acotar por {@code hospitalization_procedure_id} a secas NO prueba nada —es
     * una FK ajena, el paciente es de alguien—, así que el filtro sube por la
     * asociación hasta {@code hospitalizations.company_id}.
     */
    @EntityGraph(attributePaths = {"hospitalizationProcedure", "createdBy"})
    @Query("""
            SELECT s
            FROM ProcedureScheduleJpaEntity s
            WHERE s.hospitalizationProcedure.id = :procedureId
              AND s.hospitalizationProcedure.hospitalization.company.id = :companyId
            """)
    List<ProcedureScheduleJpaEntity> findByHospitalizationProcedureIdAndCompanyId(
            @Param("procedureId") Long procedureId, @Param("companyId") Long companyId);

    @EntityGraph(attributePaths = {"hospitalizationProcedure", "createdBy"})
    List<ProcedureScheduleJpaEntity> findByHospitalizationProcedureHospitalizationId(
            Long hospitalizationId);

    /**
     * Mismo camino a la empresa que
     * {@link #findByHospitalizationProcedureIdAndCompanyId}.
     */
    @EntityGraph(attributePaths = {"hospitalizationProcedure", "createdBy"})
    @Query("""
            SELECT s
            FROM ProcedureScheduleJpaEntity s
            WHERE s.hospitalizationProcedure.hospitalization.id = :hospitalizationId
              AND s.hospitalizationProcedure.hospitalization.company.id = :companyId
            """)
    List<ProcedureScheduleJpaEntity> findByHospitalizationIdAndCompanyId(
            @Param("hospitalizationId") Long hospitalizationId, @Param("companyId") Long companyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE procedure_schedules
            SET enabled = false
            WHERE hospitalization_procedure_id = :procedureId
              AND enabled = true
            """, nativeQuery = true)
    int disableByHospitalizationProcedureId(@Param("procedureId") Long procedureId);

    /**
     * El filtro por empresa no es defensa en profundidad: es LA defensa. Este
     * UPDATE borra el plan de ejecuciones entero de un procedimiento, y sin él
     * bastaba adivinar el {@code hospitalization_procedure_id} de otro tenant para
     * dejarle al paciente ajeno el plan en blanco.
     *
     * <p>
     * La empresa no cuelga de {@code procedure_schedules}: cuelga de la
     * hospitalización, dos saltos arriba, así que el filtro viaja por un
     * {@code EXISTS} contra {@code hospitalization_procedures} unido a
     * {@code hospitalizations} — la misma ruta que usa
     * {@code findByHospitalizationProcedureIdAndCompanyId}.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE procedure_schedules s
            SET s.enabled = false
            WHERE s.hospitalization_procedure_id = :procedureId
              AND s.enabled = true
              AND EXISTS (SELECT 1
                          FROM hospitalization_procedures p
                          JOIN hospitalizations h ON h.id = p.hospitalization_id
                          WHERE p.id = s.hospitalization_procedure_id
                            AND h.company_id = :companyId)
            """, nativeQuery = true)
    int disableByHospitalizationProcedureId(@Param("procedureId") Long procedureId,
            @Param("companyId") Long companyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE procedure_schedules
            SET enabled = false
            WHERE hospitalization_procedure_id = :procedureId
              AND enabled = true
              AND (applied_status IS NULL OR applied_status <> 'APPLIED')
            """, nativeQuery = true)
    int disablePendingByHospitalizationProcedureId(@Param("procedureId") Long procedureId);

    /**
     * Suspensión acotada al tenant, y aquí el {@code WHERE} es toda la seguridad
     * que hay: {@code SuspendPendingProcedureSchedules} no lee la ejecución antes
     * de escribir —decide qué devolver mirando lo que quedó vivo—, así que sin este
     * filtro cualquiera con {@code hospitalization.update} suspendía las
     * ejecuciones pendientes de un paciente de otro tenant. Mismo {@code EXISTS}
     * que {@link #disableByHospitalizationProcedureId(Long, Long)}.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE procedure_schedules s
            SET s.enabled = false
            WHERE s.hospitalization_procedure_id = :procedureId
              AND s.enabled = true
              AND (s.applied_status IS NULL OR s.applied_status <> 'APPLIED')
              AND EXISTS (SELECT 1
                          FROM hospitalization_procedures p
                          JOIN hospitalizations h ON h.id = p.hospitalization_id
                          WHERE p.id = s.hospitalization_procedure_id
                            AND h.company_id = :companyId)
            """, nativeQuery = true)
    int disablePendingByHospitalizationProcedureId(@Param("procedureId") Long procedureId,
            @Param("companyId") Long companyId);
}
