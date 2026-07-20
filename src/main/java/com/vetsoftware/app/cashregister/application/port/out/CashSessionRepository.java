package com.vetsoftware.app.cashregister.application.port.out;

import com.vetsoftware.app.cashregister.application.command.SearchCashSessionsQuery;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.dto.PageResult;
import com.vetsoftware.app.cashregister.domain.CashSession;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Puerto de salida del agregado de caja: persistencia y consultas de sesión. */
public interface CashSessionRepository {

    /** Persiste la sesión con sus movimientos/counts (append-only); devuelve el agregado con IDs y versión. */
    CashSession save(CashSession session);

    /** Detalle por id acotado a la empresa (con movimientos y counts hidratados). */
    Optional<CashSession> findByIdAndCompany(Long id, Long companyId);

    /** Sesión OPEN de (empresa, sede, terminal) con sus movimientos (para totales y para registrar movimientos). */
    Optional<CashSession> findOpen(Long companyId, Long branchId, String terminal);

    /** Resumen de la sesión OPEN de la terminal, incluyendo nombres de sede y responsable. */
    Optional<CashSessionView> findOpenSummary(Long companyId, Long branchId, String terminal);

    /** Resumen de la sesión OPEN asociada a la entidad terminal. */
    Optional<CashSessionView> findOpenSummaryByTerminalId(Long companyId, Long branchId, Long terminalId);

    /** ¿Hay ya una sesión OPEN para (empresa, sede, terminal)? Red de negocio antes del índice único de la BD. */
    boolean existsOpen(Long companyId, Long branchId, String terminal);

    /** ¿La entidad terminal tiene una sesión OPEN? */
    boolean existsOpenByTerminalId(Long companyId, Long branchId, Long terminalId);

    /** ¿El empleado ya tiene una sesión OPEN en cualquier sede o terminal de la empresa? */
    boolean existsOpenByEmployee(Long companyId, Long employeeId);

    /** Sesión OPEN del empleado, independientemente de su terminal. */
    Optional<CashSession> findOpenByEmployee(Long companyId, Long employeeId);

    /** Historial paginado por empresa, sede, empleado de apertura y rango, más reciente primero. */
    PageResult<CashSessionView> search(SearchCashSessionsQuery query);

    /**
     * Cajas OPEN visibles. {@code accessibleBranchIds == null} significa todas las sedes de la empresa (solo admin);
     * para empleados normales contiene exclusivamente las sedes asignadas del JWT.
     */
    List<CashSessionView> findOpenSummaries(Long companyId, Set<Long> accessibleBranchIds);
}
