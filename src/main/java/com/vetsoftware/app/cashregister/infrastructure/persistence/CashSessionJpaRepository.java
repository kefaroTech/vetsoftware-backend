package com.vetsoftware.app.cashregister.infrastructure.persistence;

import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashSessionJpaRepository extends JpaRepository<CashSessionJpaEntity, Long> {

    String SUMMARY_SELECT = """
        SELECT s.id AS id,
               s.branch_id AS branchId,
               branch.name AS branchName,
               s.terminal AS terminal,
               s.status AS status,
               s.opened_by_employee_id AS openedByEmployeeId,
               opened_by.name AS openedByEmployeeName,
               s.opened_at AS openedAt,
               s.opening_float AS openingFloat,
               CASE
                   WHEN s.status = 'CLOSED' THEN COALESCE(closing_counts.closing_total, 0)
                   ELSE NULL
               END AS closingTotal,
               s.closed_by_employee_id AS closedByEmployeeId,
               closed_by.name AS closedByEmployeeName,
               s.closed_at AS closedAt,
               s.note AS note,
               s.version AS version
          FROM cash_session s
          JOIN branches branch ON branch.id = s.branch_id
          LEFT JOIN (
              SELECT session_id, SUM(counted_amount) AS closing_total
                FROM cash_session_count
               GROUP BY session_id
          ) closing_counts ON closing_counts.session_id = s.id
          LEFT JOIN employees opened_by ON opened_by.id = s.opened_by_employee_id
          LEFT JOIN employees closed_by ON closed_by.id = s.closed_by_employee_id
        """;

    // Detalle por id + empresa. Las colecciones (movimientos/counts) se hidratan LAZY dentro de la transacción de
    // lectura al mapear a dominio (evita MultipleBagFetchException al traer dos @OneToMany a la vez).
    Optional<CashSessionJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    // Sesión OPEN de (empresa, sede, terminal). Solo puede haber una (índice único condicional en BD).
    Optional<CashSessionJpaEntity> findFirstByCompanyIdAndBranchIdAndTerminalAndStatus(
        Long companyId, Long branchId, String terminal, CashSessionStatus status);

    @Query(value = SUMMARY_SELECT + """
         WHERE s.company_id = :companyId
           AND s.branch_id = :branchId
           AND s.terminal = :terminal
           AND s.status = 'OPEN'
         LIMIT 1
        """, nativeQuery = true)
    Optional<CashSessionSummaryRow> findOpenSummary(@Param("companyId") Long companyId,
                                                     @Param("branchId") Long branchId,
                                                     @Param("terminal") String terminal);

    boolean existsByCompanyIdAndBranchIdAndTerminalAndStatus(
        Long companyId, Long branchId, String terminal, CashSessionStatus status);

    boolean existsByCompanyIdAndOpenedByEmployeeIdAndStatus(
        Long companyId, Long openedByEmployeeId, CashSessionStatus status);

    Optional<CashSessionJpaEntity> findFirstByCompanyIdAndOpenedByEmployeeIdAndStatus(
        Long companyId, Long openedByEmployeeId, CashSessionStatus status);

    // Historial por empresa y filtros opcionales de sede, empleado que abrió y fechas de apertura.
    // Los LEFT JOIN nativos incluyen el nombre aunque el empleado haya sido desactivado (EmployeeJpaEntity tiene
    // @SQLRestriction enabled=true) y evitan resolver responsables con una consulta por fila.
    @Query(value = SUMMARY_SELECT + """
         WHERE s.company_id = :companyId
           AND (:branchId IS NULL OR s.branch_id = :branchId)
           AND (:employeeId IS NULL OR s.opened_by_employee_id = :employeeId)
           AND (:from IS NULL OR s.opened_at >= :from)
           AND (:to IS NULL OR s.opened_at < :to)
         ORDER BY s.opened_at DESC
        """,
        countQuery = """
        SELECT COUNT(*)
          FROM cash_session s
         WHERE s.company_id = :companyId
           AND (:branchId IS NULL OR s.branch_id = :branchId)
           AND (:employeeId IS NULL OR s.opened_by_employee_id = :employeeId)
           AND (:from IS NULL OR s.opened_at >= :from)
           AND (:to IS NULL OR s.opened_at < :to)
        """,
        nativeQuery = true)
    Page<CashSessionSummaryRow> search(@Param("companyId") Long companyId, @Param("branchId") Long branchId,
                                       @Param("employeeId") Long employeeId, @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to, Pageable pageable);

    /** Admin: todas las cajas OPEN de la empresa, sin acotar por sede. */
    @Query(value = SUMMARY_SELECT + """
         WHERE s.company_id = :companyId
           AND s.status = 'OPEN'
         ORDER BY s.opened_at DESC
        """, nativeQuery = true)
    List<CashSessionSummaryRow> findAllOpenByCompany(@Param("companyId") Long companyId);

    /** No-admin: cajas OPEN únicamente de las sedes asignadas incluidas en el JWT. */
    @Query(value = SUMMARY_SELECT + """
         WHERE s.company_id = :companyId
           AND s.status = 'OPEN'
           AND s.branch_id IN (:branchIds)
         ORDER BY s.opened_at DESC
        """, nativeQuery = true)
    List<CashSessionSummaryRow> findAllOpenByCompanyAndBranchIdIn(
        @Param("companyId") Long companyId, @Param("branchIds") Collection<Long> branchIds);
}
