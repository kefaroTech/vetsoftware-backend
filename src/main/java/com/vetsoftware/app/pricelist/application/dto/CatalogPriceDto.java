package com.vetsoftware.app.pricelist.application.dto;

import com.vetsoftware.app.pricelist.domain.BillingCycle;
import com.vetsoftware.app.pricelist.domain.CatalogItemRef;
import com.vetsoftware.app.pricelist.domain.CatalogPrice;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @param catalogItem
 *            el articulo al que pertenece el precio, resuelto para pintarlo.
 *            Puede venir {@code null}: un precio cuyo articulo se retiro del
 *            catalogo sigue siendo una fila legitima de una tarifa historica.
 *            Ver {@code CatalogItemQueryPort} e incidencia #379.
 */
public record CatalogPriceDto(Long id, Long priceListId, Long catalogItemId,
        BillingCycle billingCycle, int tierMin, Integer tierMax, int includedQuantity,
        BigDecimal unitAmount, BigDecimal setupAmount, BigDecimal taxRate,
        TaxTreatment taxTreatment, LocalDateTime createdDate, boolean enabled,
        CatalogItemSummaryDto catalogItem) {

    /**
     * Sin resumen del articulo. Se conserva porque {@code catalogItem} se anadio
     * despues y no todos los caminos lo resuelven; equivale a pasar {@code null}.
     */
    public CatalogPriceDto(Long id, Long priceListId, Long catalogItemId, BillingCycle billingCycle,
            int tierMin, Integer tierMax, int includedQuantity, BigDecimal unitAmount,
            BigDecimal setupAmount, BigDecimal taxRate, TaxTreatment taxTreatment,
            LocalDateTime createdDate, boolean enabled) {
        this(id, priceListId, catalogItemId, billingCycle, tierMin, tierMax, includedQuantity,
                unitAmount, setupAmount, taxRate, taxTreatment, createdDate, enabled, null);
    }

    public static CatalogPriceDto from(CatalogPrice price) {
        return from(price, null);
    }

    public static CatalogPriceDto from(CatalogPrice price, CatalogItemRef catalogItem) {
        return new CatalogPriceDto(price.getId(), price.getPriceListId(), price.getCatalogItemId(),
                price.getBillingCycle(), price.getTierMin(), price.getTierMax(),
                price.getIncludedQuantity(), price.getUnitAmount(), price.getSetupAmount(),
                price.getTaxRate(), price.getTaxTreatment(), price.getCreatedDate(),
                price.isEnabled(), CatalogItemSummaryDto.from(catalogItem));
    }
}
