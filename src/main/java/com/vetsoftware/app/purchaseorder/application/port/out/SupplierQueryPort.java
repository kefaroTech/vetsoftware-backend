package com.vetsoftware.app.purchaseorder.application.port.out;

import com.vetsoftware.app.purchaseorder.domain.SupplierRef;
import java.util.Optional;

/** Outbound port hacia la feature {@code supplier}: carga el companion VO {@link SupplierRef} validando que el
 *  proveedor pertenezca a la empresa (scoped). */
public interface SupplierQueryPort {
    Optional<SupplierRef> findById(Long supplierId, Long companyId);
}
