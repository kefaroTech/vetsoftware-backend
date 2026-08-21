package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DebtOpenAccountJpaRepository
        extends
            JpaRepository<DebtOpenAccountJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"openAccount", "createdBy", "voidedBy"})
    List<DebtOpenAccountJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"openAccount", "createdBy", "voidedBy"})
    Optional<DebtOpenAccountJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"openAccount", "createdBy", "voidedBy"})
    Optional<DebtOpenAccountJpaEntity> findByIdAndOpenAccount_Company_Id(Long id, Long companyId);

    /**
     * Lectura de bloqueo sobre la fila del abono, primera sentencia de todo caso de
     * uso que lo mute. Va deliberadamente SIN {@code @EntityGraph}: la asociacion
     * {@code openAccount} se queda en proxy perezoso y leerle el identificador no
     * lo inicializa, asi que esta consulta NO mete la cuenta en el contexto de
     * persistencia con los valores viejos —si lo hiciera, el
     * {@code findByIdForUpdateAndCompanyId} posterior tomaria el lock pero
     * devolveria la instancia ya gestionada, y el saldo que leyera el guard de
     * sobrepago seguiria siendo el de antes del lock—. Mismo criterio, y por el
     * mismo motivo, que el {@code findByIdForUpdate} de
     * {@code OpenAccountJpaRepository}.
     *
     * <p>
     * Sin {@code companyId} a proposito: el filtro por empresa exige un JOIN contra
     * {@code open_accounts} y el {@code FOR UPDATE} bloquearia tambien esa fila,
     * rompiendo el orden ascendente con el que el caso de uso toma los locks de
     * cuenta. Ver {@code DebtOpenAccountRepository#lockAndFindOpenAccountId}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("""
            select d from DebtOpenAccountJpaEntity d where d.id = :id
            """)
    Optional<DebtOpenAccountJpaEntity> findByIdForUpdate(
            @org.springframework.data.repository.query.Param("id") Long id);

    @EntityGraph(attributePaths = {"openAccount", "createdBy", "voidedBy"})
    Page<DebtOpenAccountJpaEntity> findAllByOpenAccount_Company_Id(Long companyId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"openAccount", "createdBy", "voidedBy"})
    List<DebtOpenAccountJpaEntity> findByOpenAccount_IdAndOpenAccount_Company_Id(Long openAccountId,
            Long companyId);

    @EntityGraph(attributePaths = {"openAccount", "createdBy", "voidedBy"})
    Optional<DebtOpenAccountJpaEntity> findByOpenAccount_IdAndClientRequestId(Long openAccountId,
            String clientRequestId);

    // Total payments (abonos) for an open account. enabled = true is filtered
    // explicitly
    // (do not rely on @SQLRestriction for aggregate queries).
    @org.springframework.data.jpa.repository.Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM DebtOpenAccountJpaEntity d
            WHERE d.openAccount.id = :openAccountId
              AND d.enabled = true
              AND d.voided = false
            """)
    java.math.BigDecimal sumPaymentsByOpenAccountId(
            @org.springframework.data.repository.query.Param("openAccountId") Long openAccountId);
}
