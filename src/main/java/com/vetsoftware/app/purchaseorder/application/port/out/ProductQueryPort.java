package com.vetsoftware.app.purchaseorder.application.port.out;

import com.vetsoftware.app.purchaseorder.domain.ProductRef;
import java.util.Optional;

/** Outbound port hacia la feature {@code product}: carga el companion VO {@link ProductRef} validando que el
 *  producto pertenezca a la empresa (scoped). */
public interface ProductQueryPort {
    Optional<ProductRef> findById(Long productId, Long companyId);
}
