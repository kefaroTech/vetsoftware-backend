package com.vetsoftware.app.purchaseorder.application.port.out;

import com.vetsoftware.app.purchaseorder.domain.BranchRef;
import java.util.Optional;

/**
 * Outbound port hacia la feature {@code branch}: carga el companion VO
 * {@link BranchRef} validando que la sede pertenezca a la empresa (scoped).
 */
public interface BranchQueryPort {
    Optional<BranchRef> findById(Long branchId, Long companyId);
}
