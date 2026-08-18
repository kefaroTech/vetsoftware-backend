package com.vetsoftware.app.openaccount.infrastructure.persistence;

import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpenAccountJpaRepository
        extends
            JpaRepository<OpenAccountJpaEntity, Long>,
            JpaSpecificationExecutor<OpenAccountJpaEntity> {

    @Override
    @EntityGraph(attributePaths = {"owner", "company", "branch", "createdBy"})
    List<OpenAccountJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"owner", "company", "branch", "createdBy"})
    Optional<OpenAccountJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"owner", "company", "branch", "createdBy"})
    Optional<OpenAccountJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    // Multi-sucursal (Fase C): lista de la empresa con filtro OPCIONAL por sede
    // (branch es
    // @ManyToOne).
    // branchId null = todas las sedes.
    @EntityGraph(attributePaths = {"owner", "company", "branch", "createdBy"})
    @Query("""
            SELECT o
            FROM OpenAccountJpaEntity o
            WHERE o.company.id = :companyId
              AND (:branchId IS NULL OR o.branch.id = :branchId)
            """)
    Page<OpenAccountJpaEntity> findByCompanyIdAndOptionalBranch(@Param("companyId") Long companyId,
            @Param("branchId") Long branchId, Pageable pageable);

    /**
     * BE-06: contadores por pestana y saldo pendiente, en una sola consulta. La
     * pantalla los calculaba sumando el array completo; con paginacion servida esa
     * suma solo veria la pagina cargada.
     *
     * <p>
     * El saldo acumula solo cuentas OPEN: el {@code outstandingAmount} de una
     * cancelada es un monto dado de baja, no algo cobrable.
     */
    @Query("""
            SELECT COUNT(CASE WHEN o.status = :open THEN 1 END) AS openCount,
                   COUNT(CASE WHEN o.status <> :open THEN 1 END) AS closedCount,
                   SUM(CASE WHEN o.status = :open THEN o.outstandingAmount END) AS totalOutstanding
            FROM OpenAccountJpaEntity o
            WHERE o.company.id = :companyId
              AND (:branchId IS NULL OR o.branch.id = :branchId)
            """)
    OpenAccountsSummaryProjection summarize(@Param("companyId") Long companyId,
            @Param("branchId") Long branchId, @Param("open") OpenAccountStatus open);

    /**
     * Proyeccion de {@link #summarize}. {@code totalOutstanding} llega null cuando
     * no hay ninguna cuenta abierta (SUM sobre cero filas); el adaptador lo
     * normaliza a cero.
     */
    interface OpenAccountsSummaryProjection {
        long getOpenCount();

        long getClosedCount();

        BigDecimal getTotalOutstanding();
    }

    // Bloqueo pesimista de la fila de la cuenta para serializar el recálculo de
    // totales bajo
    // concurrencia
    // (cargos/abonos simultáneos). Sin @EntityGraph a propósito: FOR UPDATE no
    // combina con
    // join-fetch; las
    // asociaciones se cargan lazy dentro de la transacción.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o from OpenAccountJpaEntity o where o.id = :id
            """)
    Optional<OpenAccountJpaEntity> findByIdForUpdate(@Param("id") Long id);

    // Variante scoped a la empresa: el FOR UPDATE solo toma el lock si la fila
    // pertenece a companyId,
    // evitando bloquear (o leer) una cuenta de otro tenant. Mismo motivo del
    // sin-@EntityGraph que
    // arriba.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o from OpenAccountJpaEntity o where o.id = :id and o.company.id = :companyId
            """)
    Optional<OpenAccountJpaEntity> findByIdForUpdateAndCompanyId(@Param("id") Long id,
            @Param("companyId") Long companyId);

    // Regla "1 cuenta abierta por propietario y sede". Las CLOSE/CANCEL no bloquean
    // una nueva cuenta.
    boolean existsByOwnerIdAndBranchIdAndStatusAndEnabledTrue(Long ownerId, Long branchId,
            OpenAccountStatus status);

    /**
     * El filtro por {@code company_id} no es defensa en profundidad: es LA defensa.
     * El servicio comprobaba la empresa DESPUES del UPDATE y solo el rollback de la
     * transaccion deshacia la escritura ajena; cualquier cambio en el
     * {@code @Transactional} o en el manejo de la excepcion la habria convertido en
     * fuga silenciosa. Con el filtro, cero filas ya significa «no existe en TU
     * empresa» y no se escribe nada.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE open_accounts
            SET enabled = true
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);
}
