package com.vetsoftware.app.cashregister.application.port.out;

import java.util.Optional;

/**
 * Resuelve el catálogo de terminales sin acoplar el caso de apertura a su
 * persistencia.
 */
public interface CashTerminalQueryPort {
    Optional<TerminalRef> findActive(Long terminalId, Long companyId, Long branchId);

    record TerminalRef(Long id, String name, String code) {
    }
}
