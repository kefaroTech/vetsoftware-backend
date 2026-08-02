package com.vetsoftware.app.supplierinvoice.application.port.out;

import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import java.util.Optional;

/**
 * Carga el companion VO del proveedor (feature {@code supplier}), validando que pertenezca a la
 * empresa.
 */
public interface SupplierQueryPort {
  Optional<SupplierRef> findByIdAndCompanyId(Long supplierId, Long companyId);
}
