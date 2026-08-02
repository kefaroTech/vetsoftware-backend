package com.vetsoftware.app.productchargeopenaccount.application.port.out;

import java.util.Optional;

/**
 * Resuelve la sede que registra la salida de inventario de un cargo. Si
 * {@code requestedBranchId} viene, se valida que pertenezca a la empresa y esté
 * ACTIVA; si no, se usa la sede "Principal" por defecto. El alcance por
 * empleado (qué sedes puede usar un no-admin) ya lo aplicó el controller vía
 * {@code Authz.resolveAccessibleBranch}.
 */
public interface BranchResolverPort {
    Optional<Long> resolve(Long companyId, Long requestedBranchId);
}
