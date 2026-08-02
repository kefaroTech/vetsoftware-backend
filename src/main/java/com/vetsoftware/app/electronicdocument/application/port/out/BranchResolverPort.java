package com.vetsoftware.app.electronicdocument.application.port.out;

import java.util.Optional;

/**
 * Resuelve el id de la sucursal emisora de una venta POS. Si
 * {@code requestedBranchId} viene, se valida que pertenezca a la empresa; si
 * no, se usa la sede por defecto ("Principal"). Empresas de una sola sede
 * funcionan sin enviarlo. La emisión desde cuenta cerrada NO usa esto: hereda
 * la sede de la cuenta.
 */
public interface BranchResolverPort {
    Optional<Long> resolve(Long companyId, Long requestedBranchId);
}
