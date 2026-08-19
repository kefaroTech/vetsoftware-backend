package com.vetsoftware.app.employee.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EmployeeJpaRepository extends JpaRepository<EmployeeJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<EmployeeJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<EmployeeJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    @Query("""
            SELECT e
            FROM EmployeeJpaEntity e
            WHERE e.company.id = :companyId
            """)
    List<EmployeeJpaEntity> findAllByCompanyId(@Param("companyId") Long companyId);

    // Lista de la company INCLUYENDO desactivados (para la pantalla de empleados,
    // que muestra el
    // estado
    // Activo/Inactivo). Nativa para saltar el @SQLRestriction("enabled = true"); la
    // company se
    // hidrata
    // perezosamente al mapear (el servicio corre @Transactional).
    @Query(value = """
            SELECT *
            FROM employees
            WHERE company_id = :companyId
            ORDER BY id
            """, nativeQuery = true)
    List<EmployeeJpaEntity> findAllByCompanyIdIncludingDisabled(@Param("companyId") Long companyId);

    // Búsqueda paginada de la company INCLUYENDO desactivados (la pantalla muestra
    // el estado). Nativa
    // para saltar
    // el @SQLRestriction("enabled = true"); la company se hidrata perezosamente al
    // mapear (caller
    // @Transactional).
    // El filtro de texto es opcional (:q == null → sin filtro); LIKE usa la
    // collation CI de MySQL
    // (case-insensitive).
    @Query(value = """
            SELECT *
            FROM employees e
            WHERE e.company_id = :companyId
              AND (:q IS NULL OR e.name LIKE :q OR e.email LIKE :q OR e.employee_code LIKE :q)
            ORDER BY e.name ASC
            """, countQuery = """
            SELECT COUNT(*)
            FROM employees e
            WHERE e.company_id = :companyId
              AND (:q IS NULL OR e.name LIKE :q OR e.email LIKE :q OR e.employee_code LIKE :q)
            """, nativeQuery = true)
    Page<EmployeeJpaEntity> searchByCompanyIncludingDisabled(@Param("companyId") Long companyId,
            @Param("q") String q, Pageable pageable);

    @EntityGraph(attributePaths = "company")
    Optional<EmployeeJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    // Busca por id INCLUYENDO desactivados. Nativa para saltar el
    // @SQLRestriction("enabled = true").
    // La company se hidrata perezosamente al mapear (el caller corre en
    // transacción).
    @Query(value = """
            SELECT *
            FROM employees
            WHERE id = :id
            """, nativeQuery = true)
    Optional<EmployeeJpaEntity> findByIdIncludingDisabled(@Param("id") Long id);

    /**
     * Igual que {@link #findByIdIncludingDisabled} pero acotada al tenant. La usa
     * la baja logica: sin ella, un empleado con {@code employee.delete} desactivaba
     * al empleado de cualquier empresa con solo escribir su id en la URL, porque la
     * lectura previa no miraba de quien era la fila.
     */
    @Query(value = """
            SELECT *
            FROM employees
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    Optional<EmployeeJpaEntity> findByIdIncludingDisabledAndCompanyId(@Param("id") Long id,
            @Param("companyId") Long companyId);

    boolean existsByEmployeeCode(String employeeCode);

    // Disponibilidad real del código: cuenta TODAS las filas (incluidas las de
    // empleados
    // desactivados, que
    // el @SQLRestriction oculta pero la constraint unique de la BD sí cuenta).
    // Nativo para saltar el
    // filtro.
    // La comparación usa la collation de la columna (ci en MySQL) → es
    // case-insensitive, igual que el
    // unique.
    @Query(value = """
            SELECT COUNT(*)
            FROM employees
            WHERE employee_code = :code
            """, nativeQuery = true)
    long countByEmployeeCodeAllRows(@Param("code") String code);

    @EntityGraph(attributePaths = "company")
    Optional<EmployeeJpaEntity> findByEmployeeCode(String employeeCode);

    // "Recordar mi código": todas las cuentas activas y verificadas con ese correo
    // (de cualquier
    // company).
    // El @SQLRestriction("enabled = true") ya excluye desactivadas; la company
    // viene por
    // @EntityGraph.
    @EntityGraph(attributePaths = "company")
    List<EmployeeJpaEntity> findByEmailAndEmailVerified(String email, boolean emailVerified);

    @Query("""
            SELECT e
            FROM EmployeeJpaEntity e
            JOIN FETCH e.company c
            WHERE e.id = :id
              AND e.enabled = true
              AND c.enabled = true
            """)
    Optional<EmployeeJpaEntity> findActiveWithCompanyById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM EmployeeJpaEntity e
            JOIN FETCH e.company c
            WHERE e.id = :id
              AND e.enabled = true
              AND c.enabled = true
            """)
    Optional<EmployeeJpaEntity> findActiveWithCompanyByIdForUpdate(@Param("id") Long id);

    // El UPDATE mueve tambien `version`, la del bloqueo
    // optimista, a proposito: sin eso, un save cargado antes
    // del bump reescribe auth_version con su valor viejo
    // —el mapper la copia desde el dominio— y su
    // WHERE version = ? casa igual, con lo que una edicion
    // concurrente revalida en silencio las sesiones que la
    // reactivacion acababa de tumbar. Movida la version, ese
    // save ya no encuentra fila y salta
    // ObjectOptimisticLockingFailureException -> 409
    // CONCURRENT_MODIFICATION. `version` NO va en el WHERE:
    // dar de baja o reactivar es una operacion administrativa
    // deliberada y debe ejecutarse siempre.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE employees
            SET enabled = true, auth_version = auth_version + 1, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@Param("id") Long id);

    /**
     * Reactivacion acotada al tenant. En la reactivacion no hay lectura previa que
     * valide la propiedad —el servicio decide si existe mirando las filas
     * afectadas—, asi que este {@code AND company_id} es la unica barrera: sin el,
     * un empleado podia devolverle el login (y subirle la {@code auth_version}) a
     * alguien a quien otra empresa habia despedido.
     *
     * <p>
     * El UPDATE mueve tambien {@code version}, la del bloqueo optimista, a
     * proposito: sin eso, un save cargado antes del bump reescribe
     * {@code auth_version} con su valor viejo —el mapper la copia desde el dominio—
     * y su {@code WHERE version = ?} casa igual, con lo que una edicion concurrente
     * revalida en silencio las sesiones que la reactivacion acababa de tumbar.
     * Movida la version, ese save ya no encuentra fila y salta
     * {@code ObjectOptimisticLockingFailureException} -> 409
     * {@code CONCURRENT_MODIFICATION}. {@code version} NO va en el {@code WHERE}:
     * dar de baja o reactivar es una operacion administrativa deliberada y debe
     * ejecutarse siempre, no competir con una edicion.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE employees
            SET enabled = true, auth_version = auth_version + 1, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@Param("id") Long id, @Param("companyId") Long companyId);

    // Soft-delete por UPDATE nativo (mismo efecto que el @SQLDelete de la entidad).
    // Evita pasar por
    // el ciclo
    // de entidades: si en la misma transacción hay employee_roles gestionados
    // apuntando a este
    // empleado,
    // deleteById provocaría un TransientObjectException al flushear.
    // clearAutomatically evicta esos
    // hijos.
    //
    // El UPDATE mueve tambien `version`, la del bloqueo
    // optimista, a proposito: sin eso, un save cargado antes
    // del bump reescribe auth_version con su valor viejo
    // —el mapper la copia desde el dominio— y su
    // WHERE version = ? casa igual, con lo que una edicion
    // concurrente revalida en silencio las sesiones que la
    // baja acababa de tumbar: los tokens del despedido
    // vuelven a pasar el filtro. Movida la version, ese save
    // ya no encuentra fila y salta
    // ObjectOptimisticLockingFailureException -> 409
    // CONCURRENT_MODIFICATION. `version` NO va en el WHERE:
    // dar de baja o reactivar es una operacion administrativa
    // deliberada y debe ejecutarse siempre.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE employees
            SET enabled = false, auth_version = auth_version + 1, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int deactivate(@Param("id") Long id);

    /**
     * Baja logica acotada al tenant, simetrica de {@link #reactivate(Long, Long)}.
     * Este {@code AND company_id} es la segunda barrera del borrado: la primera es
     * la lectura previa del servicio, que ya va al finder acotado. Sin ninguna de
     * las dos, un empleado con {@code employee.delete} dejaba sin acceso al
     * personal de otra empresa —y le subia la {@code auth_version}, tumbandole las
     * sesiones vivas— con solo conocer un id.
     *
     * <p>
     * El UPDATE mueve tambien {@code version}, la del bloqueo optimista, a
     * proposito: sin eso, un save cargado antes del bump reescribe
     * {@code auth_version} con su valor viejo —el mapper la copia desde el dominio—
     * y su {@code WHERE version = ?} casa igual, con lo que una edicion concurrente
     * revalida en silencio las sesiones que la baja acababa de tumbar: basta con
     * que un administrador tuviera la ficha cargada y guarde un cambio para que los
     * tokens del despedido vuelvan a pasar el filtro. Movida la version, ese save
     * ya no encuentra fila y salta {@code ObjectOptimisticLockingFailureException}
     * -> 409 {@code CONCURRENT_MODIFICATION}. {@code version} NO va en el
     * {@code WHERE}: dar de baja o reactivar es una operacion administrativa
     * deliberada y debe ejecutarse siempre, no competir con una edicion.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE employees
            SET enabled = false, auth_version = auth_version + 1, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int deactivate(@Param("id") Long id, @Param("companyId") Long companyId);

    // Invalida los access tokens vivos del empleado sin tocar `enabled`.
    //
    // Sin acotar: es el camino del refresh (RefreshTokenUseCase, que es
    // @NoAuthorizationRequired), donde el sujeto sale del refresh token ya validado
    // y no hay empresa en el contexto. El logout usa la sobrecarga acotada.
    //
    // El UPDATE mueve tambien `version`, la del bloqueo
    // optimista, a proposito: sin eso, un save cargado antes
    // del bump reescribe auth_version con su valor viejo
    // —el mapper la copia desde el dominio— y su
    // WHERE version = ? casa igual, con lo que una edicion
    // concurrente revalida en silencio una sesion ya revocada.
    // Movida la version, ese save ya no encuentra fila y salta
    // ObjectOptimisticLockingFailureException -> 409
    // CONCURRENT_MODIFICATION. `version` NO va en el WHERE:
    // revocar es deliberado y debe ejecutarse siempre.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE employees
            SET auth_version = auth_version + 1, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int bumpAuthVersion(@Param("id") Long id);

    /**
     * Invalidacion acotada al tenant, para el camino de logout. Su puerto
     * ({@code LogoutUseCase}) es {@code @PreAuthorize("isAuthenticated()")}, el
     * gate mas debil del proyecto: no dice nada sobre de quien es la fila, solo que
     * hay alguien autenticado. El {@code companyId} sale del
     * {@code EmployeeContext} del principal, asi que el {@code AND} exige que el
     * empleado cuyas sesiones se tumban sea de la empresa de quien pide el logout.
     *
     * <p>
     * El UPDATE mueve tambien {@code version}, la del bloqueo optimista, a
     * proposito: sin eso, un save cargado antes del bump reescribe
     * {@code auth_version} con su valor viejo —el mapper la copia desde el dominio—
     * y su {@code WHERE version = ?} casa igual, con lo que una edicion concurrente
     * revalida en silencio una sesion ya revocada. Movida la version, ese save ya
     * no encuentra fila y salta {@code ObjectOptimisticLockingFailureException} ->
     * 409 {@code CONCURRENT_MODIFICATION}. {@code version} NO va en el
     * {@code WHERE}: revocar es deliberado y debe ejecutarse siempre, no competir
     * con una edicion.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE employees
            SET auth_version = auth_version + 1, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int bumpAuthVersion(@Param("id") Long id, @Param("companyId") Long companyId);

    // Primer login del staff invitado: INVITED → ACTIVE. Solo toca filas invitadas
    // (idempotente y sin
    // pisar
    // empleados ya activos). Nativa para saltar el @SQLRestriction y actualizar por
    // id directamente.
    //
    // El companyId NO sale de esta misma fila: viene de EmployeeCredentials, que
    // LoginEmployeeService leyo antes con findByCode. Son dos consultas distintas,
    // asi que el AND compara dos lecturas independientes y no es una tautologia.
    // Sin sobrecarga ancha a proposito: el unico llamador tiene la empresa en la
    // mano, y dejar viva la version sin filtro solo servia para que la copiara el
    // siguiente.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE employees
            SET status = 'ACTIVE'
            WHERE id = :id
              AND company_id = :companyId
              AND status = 'INVITED'
            """, nativeQuery = true)
    int activateInvited(@Param("id") Long id, @Param("companyId") Long companyId);

    boolean existsByCompany_Id(Long companyId);
}
