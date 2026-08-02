package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.SaleLineKind;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort.SalePromotion;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort.ValueType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calcula el precio unitario canonico (post-promo) de una linea de catalogo, espejando exactamente
 * la logica del front (tienda/composables/pricing.ts#applyPromo): toma el menor entre el precio de
 * lista y el de cada promocion activa que aplique al item (por id directo) o a su categoria, y
 * redondea a peso entero (HALF_UP, igual que Math.round). Es la fuente de verdad del precio fiscal:
 * el unitPrice del cliente se valida contra esto, no al reves.
 */
final class PromotionPriceCalculator {
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private PromotionPriceCalculator() {}

  static BigDecimal expectedUnitPrice(
      BigDecimal basePrice,
      SaleLineKind kind,
      Long refId,
      Long categoryId,
      List<SalePromotion> promos) {
    BigDecimal best = basePrice;
    for (SalePromotion p : promos) {
      if (!matches(p, kind, refId, categoryId)) continue;
      BigDecimal candidate = candidate(p, basePrice);
      if (candidate.compareTo(best) < 0) best = candidate;
    }
    return best.setScale(0, RoundingMode.HALF_UP); // espeja Math.round(best) del front
  }

  private static boolean matches(SalePromotion p, SaleLineKind kind, Long refId, Long categoryId) {
    return switch (p.applicationType()) {
      case PRODUCT -> kind == SaleLineKind.PRODUCT && p.applicationItem().equals(refId);
      case SERVICE -> kind == SaleLineKind.SERVICE && p.applicationItem().equals(refId);
      case CATEGORY -> categoryId != null && p.applicationItem().equals(categoryId);
    };
  }

  private static BigDecimal candidate(SalePromotion p, BigDecimal basePrice) {
    return switch (p.promotionType()) {
      case SPECIAL_PRICE -> p.value();
      case DISCOUNT ->
          p.valueType() == ValueType.PERCENTAGE
              ? basePrice.multiply(
                  BigDecimal.ONE.subtract(p.value().divide(HUNDRED, 10, RoundingMode.HALF_UP)))
              : basePrice.subtract(p.value()).max(BigDecimal.ZERO);
    };
  }
}
