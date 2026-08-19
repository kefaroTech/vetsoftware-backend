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

    /**
     * Regeneración completa: deshabilita todas las ejecuciones vivas del
     * procedimiento antes de reescribirlas. Camino SYSTEM; la variante acotada al
     * tenant es {@link #disableByHospitalizationProcedureId(Long, Long)}.
     *
     * <p>
     * <strong>El UPDATE mueve también {@code version}, la del bloqueo
     * optimista.</strong> Un {@code @Version} solo protege el ciclo
     * leer→modificar→guardar de una entidad gestionada por Hibernate; esta consulta
     * va directa a la base de datos, así que ni comprueba la versión ni la
     * incrementa, y el candado queda ciego por este camino. El escenario que eso
     * permitía es real y no teórico: el auxiliar A carga una ejecución para
     * marcarla como aplicada ({@code ApplyProcedureScheduleService} hace
     * {@code findById} → {@code apply} → {@code save}), entretanto un
     * {@code GenerateProcedureSchedule} concurrente ejecuta este UPDATE y deja la
     * fila en {@code enabled = false}, y el {@code save} de A —que reescribe la
     * entidad entera desde el dominio, {@code enabled} incluido— encuentra su
     * {@code WHERE version = ?} intacto y <em>resucita</em> la ejecución
     * deshabilitada. El plan borrado reaparece a medias, sin excepción y sin log.
     * Movida la versión, ese {@code save} ya no encuentra fila y salta
     * {@code ObjectOptimisticLockingFailureException} → 409
     * {@code CONCURRENT_MODIFICATION}, que es lo que el front necesita para
     * recargar y reintentar sobre datos frescos.
     *
     * <p>
     * {@code version} NO va en el {@code WHERE}: deshabilitar en cascada es una
     * operación deliberada y debe ejecutarse siempre, no competir con una edición.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE procedure_schedules
            SET enabled = false, version = version + 1
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
     *
     * <p>
     * Mueve {@code version} por lo mismo que la sobrecarga ancha —ver
     * {@link #disableByHospitalizationProcedureId(Long)}—: sin eso un {@code save}
     * cargado antes resucita la ejecución deshabilitada en silencio. El
     * {@code EXISTS} del {@code WHERE} es el gate de tenant y es cosa aparte; la
     * versión no entra en el {@code WHERE}.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE procedure_schedules s
            SET s.enabled = false, s.version = s.version + 1
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

    /**
     * Suspende solo las ejecuciones pendientes; las aplicadas son histórico
     * inmutable. Camino SYSTEM de {@code SuspendPendingProcedureSchedulesService};
     * la variante acotada al tenant es
     * {@link #disablePendingByHospitalizationProcedureId(Long, Long)}.
     *
     * <p>
     * <strong>Este par de casos de uso es el que motivó BE-53</strong>, y por eso
     * el UPDATE mueve {@code version}. {@code SuspendPendingProcedureSchedules} y
     * {@code ApplyProcedureSchedule} pueden caer sobre la <em>misma fila</em>: la
     * ejecución que A está marcando como aplicada es exactamente una de las
     * pendientes que el segundo suspende. Antes no había conflicto alguno —esta
     * consulta no tocaba la versión, así que la copia de A seguía siendo la
     * vigente— y el {@code save} de A reponía {@code enabled = true} sobre la fila
     * recién suspendida, porque el mapper reescribe la entidad entera desde el
     * dominio. La suspensión desaparecía sin excepción y sin log, y el plan quedaba
     * mostrando como viva una ejecución que el veterinario había cancelado. Movida
     * la versión, ese {@code save} ya no encuentra fila y salta
     * {@code ObjectOptimisticLockingFailureException} → 409
     * {@code CONCURRENT_MODIFICATION}: A recarga y decide sobre datos frescos, que
     * es la resolución correcta de esa carrera.
     *
     * <p>
     * {@code version} NO va en el {@code WHERE}: suspender es una operación
     * deliberada y debe ejecutarse siempre, no competir con una edición.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE procedure_schedules
            SET enabled = false, version = version + 1
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
     *
     * <p>
     * Es la consulta que ejecuta de verdad el caso de uso del tenant, así que la
     * carrera contra {@code ApplyProcedureSchedule} descrita en
     * {@link #disablePendingByHospitalizationProcedureId(Long)} es <em>esta</em>:
     * sin el {@code version = version + 1}, el {@code save} del auxiliar reponía
     * {@code enabled = true} sobre la ejecución recién suspendida y la suspensión
     * se perdía en silencio. La versión no entra en el {@code WHERE} —suspender
     * debe ejecutarse siempre—, y el {@code EXISTS} de tenant queda intacto.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE procedure_schedules s
            SET s.enabled = false, s.version = s.version + 1
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
