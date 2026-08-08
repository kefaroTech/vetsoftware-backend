package com.vetsoftware.app.openaccount.infrastructure.persistence;

import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import jakarta.persistence.LockModeType;
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

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE open_accounts
            SET enabled = true
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
