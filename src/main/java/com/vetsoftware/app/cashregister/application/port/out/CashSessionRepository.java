package com.vetsoftware.app.cashregister.application.port.out;

import com.vetsoftware.app.cashregister.application.command.SearchCashSessionsQuery;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.dto.PageResult;
import com.vetsoftware.app.cashregister.domain.CashSession;
import java.util.Optional;

/** Puerto de salida del agregado de caja: persistencia y consultas de sesión. */
public interface CashSessionRepository {

    /** Persiste la sesión con sus movimientos/counts (append-only); devuelve el agregado con IDs y versión. */
    CashSession save(CashSession session);

    /** Detalle por id acotado a la empresa (con movimientos y counts hidratados). */
    Optional<CashSession> findByIdAndCompany(Long id, Long companyId);

    /** Sesión OPEN de (empresa, sede, terminal) con sus movimientos (para totales y para registrar movimientos). */
    Optional<CashSession> findOpen(Long companyId, Long branchId, String terminal);

    /** ¿Hay ya una sesión OPEN para (empresa, sede, terminal)? Red de negocio antes del índice único de la BD. */
    boolean existsOpen(Long companyId, Long branchId, String terminal);

    /** Historial paginado por (empresa, [sede], [rango]), más reciente primero (resumen, sin movimientos). */
    PageResult<CashSessionView> search(SearchCashSessionsQuery query);
}
