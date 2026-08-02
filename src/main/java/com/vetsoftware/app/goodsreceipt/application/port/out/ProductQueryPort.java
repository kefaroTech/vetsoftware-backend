package com.vetsoftware.app.goodsreceipt.application.port.out;

import com.vetsoftware.app.goodsreceipt.domain.ProductRef;
import java.util.Optional;

/**
 * Carga el companion VO {@link ProductRef} validando que el producto pertenezca
 * a la empresa (scoped).
 */
public interface ProductQueryPort {
    Optional<ProductRef> findById(Long productId, Long companyId);
}
