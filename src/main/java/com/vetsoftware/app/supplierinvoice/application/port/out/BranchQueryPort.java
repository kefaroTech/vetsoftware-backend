package com.vetsoftware.app.supplierinvoice.application.port.out;

import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import java.util.Optional;

/**
 * Carga el companion VO de la sede (feature {@code branch}), validando que pertenezca a la empresa.
 */
public interface BranchQueryPort {
  Optional<BranchRef> findByIdAndCompanyId(Long branchId, Long companyId);
}
