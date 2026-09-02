package com.vetsoftware.app.cashregister.application.port.out;

import com.vetsoftware.app.cashregister.application.command.SearchCashSessionsQuery;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import com.vetsoftware.app.cashregister.domain.CashSession;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Puerto de salida del agregado de caja: persistencia y consultas de sesión.
 */
public interface CashSessionRepository {

    /**
     * Persiste la sesión con sus movimientos/counts (append-only); devuelve el
     * agregado con IDs y versión.
     */
    CashSession save(CashSession session);

    /**
     * Detalle por id acotado a la empresa (con movimientos y counts hidratados).
     */
    Optional<CashSession> findByIdAndCompany(Long id, Long companyId);

    /**
     * Sesión OPEN de (empresa, sede, terminal) con sus movimientos (para totales y
     * para registrar movimientos).
     */
    Optional<CashSession> findOpen(Long companyId, Long branchId, String terminal);

    /**
     * <b>La sesión donde entró el dinero que ahora se devuelve.</b> Busca la caja
     * que contiene el movimiento de ingreso orquestado ({@code SALE_IN} /
     * {@code OPEN_ACCOUNT_IN}) de esa referencia.
     *
     * <p>
     * Es la única respuesta trazable a «¿contra qué caja compenso esta anulación?»:
     * una nota crédito revierte una venta concreta, y esa venta ya tiene su sesión.
     * Compensar ahí cuadra el mismo arqueo que se descuadró, y no depende de que la
     * operación traiga actor ni de la cadena mutable del terminal —que es lo que
     * antes elegía una caja arbitraria cuando dos terminales compartían código—.
     *
     * <p>
     * Devuelve la sesión en cualquier estado: si está cerrada, quien llama tiene
     * que decidir y decirlo, no caer a otra caja en silencio.
     */
    Optional<CashSession> findSessionOfReferencedInflow(Long companyId,
            CashReferenceType referenceType, Long referenceId);

    /** Resumen de la sesión OPEN asociada a la entidad terminal. */
    Optional<CashSessionView> findOpenSummaryByTerminalId(Long companyId, Long branchId,
            Long terminalId);

    /**
     * ¿Hay ya una sesión OPEN para (empresa, sede, terminal)? Red de negocio antes
     * del índice único de la BD.
     */
    boolean existsOpen(Long companyId, Long branchId, String terminal);

    /** ¿La entidad terminal tiene una sesión OPEN? */
    boolean existsOpenByTerminalId(Long companyId, Long branchId, Long terminalId);

    /**
     * ¿El empleado ya tiene una sesión OPEN en cualquier sede o terminal de la
     * empresa?
     */
    boolean existsOpenByEmployee(Long companyId, Long employeeId);

    /** Sesión OPEN del empleado, independientemente de su terminal. */
    Optional<CashSession> findOpenByEmployee(Long companyId, Long employeeId);

    /**
     * Historial paginado por empresa, sede, empleado de apertura y rango, más
     * reciente primero.
     */
    PageResult<CashSessionView> search(SearchCashSessionsQuery query);

    /**
     * Cajas OPEN visibles. {@code accessibleBranchIds == null} significa todas las
     * sedes de la empresa (solo admin); para empleados normales contiene
     * exclusivamente las sedes asignadas del JWT.
     */
    List<CashSessionView> findOpenSummaries(Long companyId, Set<Long> accessibleBranchIds);
}
