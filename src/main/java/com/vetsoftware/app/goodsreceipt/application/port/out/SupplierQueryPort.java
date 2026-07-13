package com.vetsoftware.app.goodsreceipt.application.port.out;

import com.vetsoftware.app.goodsreceipt.domain.SupplierRef;
import java.util.Optional;

/** Carga el companion VO {@link SupplierRef} validando que el proveedor pertenezca a la empresa (scoped). */
public interface SupplierQueryPort {
    Optional<SupplierRef> findById(Long supplierId, Long companyId);
}
