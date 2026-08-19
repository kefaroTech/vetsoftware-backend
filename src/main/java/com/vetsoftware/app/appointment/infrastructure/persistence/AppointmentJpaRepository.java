package com.vetsoftware.app.appointment.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AppointmentJpaRepository extends JpaRepository<AppointmentJpaEntity, Long> {

    @EntityGraph(attributePaths = {"animal", "owner", "employee", "company", "branch"})
    Optional<AppointmentJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"animal", "owner", "employee", "company", "branch"})
    @Query("""
            SELECT a
            FROM AppointmentJpaEntity a
            WHERE a.company.id = :companyId
              AND (:from IS NULL OR a.startAt >= :from)
              AND (:to IS NULL OR a.startAt <= :to)
              AND (:employeeId IS NULL OR a.employee.id = :employeeId)
              AND (:status IS NULL OR a.status = :status)
              AND (:branchId IS NULL OR a.branch.id = :branchId)
            ORDER BY a.startAt ASC
            """)
    List<AppointmentJpaEntity> findByFilters(@Param("companyId") Long companyId,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            @Param("employeeId") Long employeeId, @Param("status") String status,
            @Param("branchId") Long branchId);

    /**
     * Candidatas a cruzarse con el intervalo {@code [startAt, endAt)} del mismo
     * veterinario (BE-17). Devuelve un <strong>superconjunto</strong>: el cruce
     * exacto lo decide {@code JpaAppointmentRepository}, en Java.
     *
     * <p>
     * <strong>Por qué la aritmética no está aquí.</strong> El fin de cada cita
     * existente es {@code startAt + coalesce(durationMinutes, default)}, y
     * calcularlo en la consulta obliga a {@code timestampadd}/{@code DATE_ADD}. En
     * este repositorio no hay un solo precedente de aritmética de fechas en JPQL
     * —los dos casos que existen son queries nativas— y, sobre todo,
     * {@code JpaAppointmentRepository} no tiene rodaja {@code @DataJpaTest} (deuda
     * congelada de BE-10): un predicado metido en el SQL no lo ejercitaría nadie.
     * La condición delicada es justo el <em>semiabierto</em> —10:00-10:30 y
     * 10:30-11:00 no se cruzan—, así que vive donde un test unitario sin base de
     * datos puede fijarla.
     *
     * <p>
     * Las dos cotas sobre {@code startAt} sí van aquí porque son las que hacen la
     * consulta sargable y no pierden ninguna fila:
     * <ul>
     * <li>{@code a.startAt < :endAt} — lo que empieza cuando ya terminamos no se
     * cruza (semiabierto: el {@code <} es estricto);
     * <li>{@code a.startAt >= :earliestStartAt} — como el dominio topa la duración
     * en {@code Appointment.MAX_DURATION_MINUTES}, lo que empezó antes de esa cota
     * ya terminó seguro.
     * </ul>
     * En la práctica deja 0-3 filas: mismo vet, misma empresa, ventana de horas.
     *
     * <p>
     * Los estados que no ocupan agenda llegan como parámetro desde
     * {@code AppointmentStatus} en vez de ir escritos a mano en el JPQL. La
     * semántica es la de siempre: fuera {@code CANCELLED} y {@code NO_SHOW}, y
     * {@code COMPLETED} <strong>sí</strong> cuenta como choque.
     */
    @Query("""
            SELECT a.id AS id, a.startAt AS startAt, a.durationMinutes AS durationMinutes,
                   a.branch.id AS branchId
            FROM AppointmentJpaEntity a
            WHERE a.company.id = :companyId
              AND a.employee.id = :employeeId
              AND a.status NOT IN :ignoredStatuses
              AND (:excludeId IS NULL OR a.id <> :excludeId)
              AND a.startAt >= :earliestStartAt
              AND a.startAt < :endAt
            ORDER BY a.startAt ASC
            """)
    List<AppointmentSlotProjection> findOverlapCandidates(@Param("companyId") Long companyId,
            @Param("employeeId") Long employeeId,
            @Param("earliestStartAt") LocalDateTime earliestStartAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("ignoredStatuses") Collection<String> ignoredStatuses,
            @Param("excludeId") Long excludeId);

    /**
     * Lo mínimo para decidir un cruce —cuándo empieza y cuánto dura— más la sede,
     * que no interviene en el cálculo pero sí en <em>cuánto de él se le puede
     * contar</em> al caller: el cruce es por veterinario, la visibilidad es por
     * sede. {@code a.branch.id} se lee de la FK, sin JOIN.
     */
    interface AppointmentSlotProjection {
        Long getId();

        LocalDateTime getStartAt();

        /** {@code null} = la cita hereda la duración por defecto de la empresa. */
        Integer getDurationMinutes();

        Long getBranchId();
    }

    /**
     * Baja lógica por query nativa, mismo efecto que el {@code @SQLDelete} de la
     * entidad. Sigue sin pasar por {@code em.remove} por la razón de siempre —evita
     * el conflicto {@code @SQLDelete} + {@code @Version}, que obliga a Hibernate a
     * ligar dos parámetros (id y versión) tomados de una entidad gestionada que
     * aquí no se carga—, pero desde BE-53 hace además una segunda cosa:
     * <strong>mantiene vivo el candado optimista.</strong>
     *
     * <p>
     * Las dos razones son independientes y conviene no confundirlas. Un
     * {@code @Version} solo protege el ciclo leer→modificar→guardar de una entidad
     * gestionada; este UPDATE va directo a la base de datos, así que por sí mismo
     * ni comprueba la versión ni la incrementa. Sin el
     * {@code version = version + 1}, un {@code save} cargado antes de la baja
     * reescribe la fila entera desde el dominio —el mapper la copia campo a campo,
     * {@code enabled} incluido— y su {@code WHERE version = ?} casa igual, con lo
     * que la cita borrada <em>reaparece</em> sin excepción y sin log. Movida la
     * versión, ese {@code save} ya no encuentra fila y salta
     * {@code ObjectOptimisticLockingFailureException} → 409
     * {@code CONCURRENT_MODIFICATION}, que es lo que el front necesita para
     * recargar y reintentar sobre datos frescos.
     *
     * <p>
     * {@code version} NO va en el {@code WHERE}: borrar es una operación deliberada
     * y debe ejecutarse siempre, no competir con una edición. El
     * {@code AND company_id} sí es el gate de tenant y no se toca.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE appointments
            SET enabled = false, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int softDelete(@Param("id") Long id, @Param("companyId") Long companyId);
}
