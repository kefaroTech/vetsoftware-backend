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
    @Query("SELECT e FROM EmployeeJpaEntity e WHERE e.company.id = :companyId")
    List<EmployeeJpaEntity> findAllByCompanyId(@Param("companyId") Long companyId);

    // Lista de la company INCLUYENDO desactivados (para la pantalla de empleados, que muestra el estado
    // Activo/Inactivo). Nativa para saltar el @SQLRestriction("enabled = true"); la company se hidrata
    // perezosamente al mapear (el servicio corre @Transactional).
    @Query(value = "SELECT * FROM employees WHERE company_id = :companyId ORDER BY id", nativeQuery = true)
    List<EmployeeJpaEntity> findAllByCompanyIdIncludingDisabled(@Param("companyId") Long companyId);

    // Búsqueda paginada de la company INCLUYENDO desactivados (la pantalla muestra el estado). Nativa para saltar
    // el @SQLRestriction("enabled = true"); la company se hidrata perezosamente al mapear (caller @Transactional).
    // El filtro de texto es opcional (:q == null → sin filtro); LIKE usa la collation CI de MySQL (case-insensitive).
    @Query(value = "SELECT * FROM employees e WHERE e.company_id = :companyId "
        + "AND (:q IS NULL OR e.name LIKE :q OR e.email LIKE :q OR e.employee_code LIKE :q) "
        + "ORDER BY e.name ASC",
        countQuery = "SELECT COUNT(*) FROM employees e WHERE e.company_id = :companyId "
        + "AND (:q IS NULL OR e.name LIKE :q OR e.email LIKE :q OR e.employee_code LIKE :q)",
        nativeQuery = true)
    Page<EmployeeJpaEntity> searchByCompanyIncludingDisabled(@Param("companyId") Long companyId,
                                                             @Param("q") String q, Pageable pageable);

    @EntityGraph(attributePaths = "company")
    Optional<EmployeeJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    // Busca por id INCLUYENDO desactivados. Nativa para saltar el @SQLRestriction("enabled = true").
    // La company se hidrata perezosamente al mapear (el caller corre en transacción).
    @Query(value = "SELECT * FROM employees WHERE id = :id", nativeQuery = true)
    Optional<EmployeeJpaEntity> findByIdIncludingDisabled(@Param("id") Long id);

    boolean existsByEmployeeCode(String employeeCode);

    // Disponibilidad real del código: cuenta TODAS las filas (incluidas las de empleados desactivados, que
    // el @SQLRestriction oculta pero la constraint unique de la BD sí cuenta). Nativo para saltar el filtro.
    // La comparación usa la collation de la columna (ci en MySQL) → es case-insensitive, igual que el unique.
    @Query(value = "SELECT COUNT(*) FROM employees WHERE employee_code = :code", nativeQuery = true)
    long countByEmployeeCodeAllRows(@Param("code") String code);

    @EntityGraph(attributePaths = "company")
    Optional<EmployeeJpaEntity> findByEmployeeCode(String employeeCode);

    // "Recordar mi código": todas las cuentas activas y verificadas con ese correo (de cualquier company).
    // El @SQLRestriction("enabled = true") ya excluye desactivadas; la company viene por @EntityGraph.
    @EntityGraph(attributePaths = "company")
    List<EmployeeJpaEntity> findByEmailAndEmailVerified(String email, boolean emailVerified);

    @Query("SELECT e FROM EmployeeJpaEntity e JOIN FETCH e.company c WHERE e.id = :id AND e.enabled = true AND c.enabled = true")
    Optional<EmployeeJpaEntity> findActiveWithCompanyById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EmployeeJpaEntity e JOIN FETCH e.company c WHERE e.id = :id AND e.enabled = true AND c.enabled = true")
    Optional<EmployeeJpaEntity> findActiveWithCompanyByIdForUpdate(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE employees SET enabled = true, auth_version = auth_version + 1 WHERE id = :id", nativeQuery = true)
    int reactivate(@Param("id") Long id);

    // Soft-delete por UPDATE nativo (mismo efecto que el @SQLDelete de la entidad). Evita pasar por el ciclo
    // de entidades: si en la misma transacción hay employee_roles gestionados apuntando a este empleado,
    // deleteById provocaría un TransientObjectException al flushear. clearAutomatically evicta esos hijos.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE employees SET enabled = false, auth_version = auth_version + 1 WHERE id = :id", nativeQuery = true)
    int deactivate(@Param("id") Long id);

    // Invalida los access tokens vivos del empleado (usado en logout) sin tocar `enabled`.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE employees SET auth_version = auth_version + 1 WHERE id = :id", nativeQuery = true)
    int bumpAuthVersion(@Param("id") Long id);

    // Primer login del staff invitado: INVITED → ACTIVE. Solo toca filas invitadas (idempotente y sin pisar
    // empleados ya activos). Nativa para saltar el @SQLRestriction y actualizar por id directamente.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE employees SET status = 'ACTIVE' WHERE id = :id AND status = 'INVITED'", nativeQuery = true)
    int activateInvited(@Param("id") Long id);

    boolean existsByCompany_Id(Long companyId);
}
