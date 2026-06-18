package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Lee del catalogo (product/service) la clasificacion tributaria AUTORITATIVA de una linea de venta POS:
 * el front manda el precio (post-promo, IVA incluido) pero el esquema/tasa los pone el servidor desde el
 * catalogo, no el cliente. Unico punto que conoce las features product/service para la venta directa.
 */
public interface CatalogLineQueryPort {
    Optional<CatalogItem> findProduct(Long productId, Long companyId);

    Optional<CatalogItem> findService(Long serviceId, Long companyId);

    /** Nombre + clasificacion tributaria congelable de un item del catalogo. taxScheme/taxRate null si no tributa. */
    record CatalogItem(String name, TaxCategory taxCategory, TaxScheme taxScheme, BigDecimal taxRate) {}
}
