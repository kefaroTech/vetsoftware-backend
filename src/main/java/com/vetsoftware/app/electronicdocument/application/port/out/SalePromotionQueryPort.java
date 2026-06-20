package com.vetsoftware.app.electronicdocument.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lee las promociones ACTIVAS (estado ACTIVE + fecha dentro de rango + enabled) de una empresa en una
 * forma local a electronicdocument, sin acoplar los enums de la feature promotion. El servidor usa estas
 * promociones para recalcular el precio canonico de cada linea POS y validar el unitPrice del cliente.
 */
public interface SalePromotionQueryPort {
    List<SalePromotion> findActive(Long companyId, LocalDate today);

    enum PromotionType { DISCOUNT, SPECIAL_PRICE }

    enum ApplicationType { CATEGORY, PRODUCT, SERVICE }

    enum ValueType { PERCENTAGE, VALUE }

    /** Promocion activa en forma local: a quien aplica (item/categoria) y el efecto sobre el precio. */
    record SalePromotion(PromotionType promotionType, ApplicationType applicationType,
                         Long applicationItem, ValueType valueType, BigDecimal value) {}
}
