package com.vetsoftware.app.product.application.port.out;

import com.vetsoftware.app.product.domain.SupplierRef;
import java.util.Optional;

/**
 * Outbound port hacia la feature {@code supplier}: carga el companion VO
 * {@link SupplierRef} validando que el proveedor pertenezca a la empresa
 * (scoped) en una sola query. Único cruce permitido: el adapter en
 * {@code infrastructure/persistence} consulta el {@code
 * SupplierJpaRepository} de la otra feature.
 */
public interface SupplierQueryPort {
    Optional<SupplierRef> findById(Long supplierId, Long companyId);
}
