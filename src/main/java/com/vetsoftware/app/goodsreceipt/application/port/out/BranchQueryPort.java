package com.vetsoftware.app.goodsreceipt.application.port.out;

import com.vetsoftware.app.goodsreceipt.domain.BranchRef;
import java.util.Optional;

/**
 * Carga el companion VO {@link BranchRef} validando que la sede pertenezca a la
 * empresa (scoped).
 */
public interface BranchQueryPort {
    Optional<BranchRef> findById(Long branchId, Long companyId);
}
