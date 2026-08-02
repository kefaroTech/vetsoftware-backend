package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Lee del catalogo (product/service) la clasificacion tributaria AUTORITATIVA
 * de una linea de venta POS y su precio de lista. El servidor pone el
 * esquema/tasa y recalcula el precio canonico (lista + promo) para validar el
 * unitPrice que manda el cliente, no al reves. Unico punto que conoce las
 * features product/service para la venta directa.
 */
public interface CatalogLineQueryPort {
    Optional<CatalogItem> findProduct(Long productId, Long companyId);

    Optional<CatalogItem> findService(Long serviceId, Long companyId);

    /**
     * Nombre + clasificacion tributaria congelable de un item del catalogo.
     * taxScheme/taxRate null si no tributa. basePrice es el precio de lista
     * (salePrice/price, IVA incluido) y categoryId la categoria del item, ambos
     * usados para recalcular el precio canonico con las promociones activas.
     */
    record CatalogItem(String name, TaxCategory taxCategory, TaxScheme taxScheme,
            BigDecimal taxRate, BigDecimal basePrice, Long categoryId) {
    }
}
