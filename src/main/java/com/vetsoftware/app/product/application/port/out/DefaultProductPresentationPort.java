package com.vetsoftware.app.product.application.port.out;

import java.math.BigDecimal;

public interface DefaultProductPresentationPort {
    void ensureDefault(Long productId, Long companyId, String unitMeasureCode, BigDecimal salePrice);

    void synchronizeDefault(
        Long productId, Long companyId, String unitMeasureCode, BigDecimal salePrice, Long actorId);
}
