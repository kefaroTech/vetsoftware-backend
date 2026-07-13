package com.vetsoftware.app.cashregister.infrastructure.persistence;

import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashSessionJpaRepository extends JpaRepository<CashSessionJpaEntity, Long> {

    // Detalle por id + empresa. Las colecciones (movimientos/counts) se hidratan LAZY dentro de la transacción de
    // lectura al mapear a dominio (evita MultipleBagFetchException al traer dos @OneToMany a la vez).
    Optional<CashSessionJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    // Sesión OPEN de (empresa, sede, terminal). Solo puede haber una (índice único condicional en BD).
    Optional<CashSessionJpaEntity> findFirstByCompanyIdAndBranchIdAndTerminalAndStatus(
        Long companyId, Long branchId, String terminal, CashSessionStatus status);

    boolean existsByCompanyIdAndBranchIdAndTerminalAndStatus(
        Long companyId, Long branchId, String terminal, CashSessionStatus status);

    // Historial por empresa y (opcional) sede + rango de fechas sobre la apertura, más reciente primero.
    @Query("SELECT s FROM CashSessionJpaEntity s WHERE s.companyId = :companyId "
        + "AND (:branchId IS NULL OR s.branchId = :branchId) "
        + "AND (:from IS NULL OR s.openedAt >= :from) "
        + "AND (:to IS NULL OR s.openedAt < :to) ORDER BY s.openedAt DESC")
    Page<CashSessionJpaEntity> search(@Param("companyId") Long companyId, @Param("branchId") Long branchId,
                                      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                      Pageable pageable);
}
