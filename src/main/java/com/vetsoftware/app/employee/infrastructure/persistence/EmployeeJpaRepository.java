package com.vetsoftware.app.employee.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query("SELECT e FROM EmployeeJpaEntity e JOIN FETCH e.company c WHERE e.id = :id AND e.enabled = true AND c.enabled = true")
    Optional<EmployeeJpaEntity> findActiveWithCompanyById(@Param("id") Long id);

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
