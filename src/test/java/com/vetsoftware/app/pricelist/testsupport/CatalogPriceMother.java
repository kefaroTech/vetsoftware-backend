package com.vetsoftware.app.pricelist.testsupport;

import com.vetsoftware.app.pricelist.domain.BillingCycle;
import com.vetsoftware.app.pricelist.domain.CatalogPrice;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class CatalogPriceMother {

    public static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 1, 15, 10, 30);
    public static final Long LISTA = 1L;
    public static final Long ARTICULO = 42L;

    private CatalogPriceMother() {
    }

    /** Precio mensual gravado al 19 %, tramo abierto desde 1. */
    public static CatalogPrice mensualGravado() {
        return conTramo(10L, 1, null);
    }

    /** Precio con el tramo que se le pida y el id que se le pida. */
    public static CatalogPrice conTramo(Long id, int tierMin, Integer tierMax) {
        return new CatalogPrice(id, LISTA, ARTICULO, BillingCycle.MONTHLY, tierMin, tierMax, 2,
                new BigDecimal("12000.00"), new BigDecimal("0.00"), new BigDecimal("19.00"),
                TaxTreatment.TAXED, CREADO_EL, 0L, true);
    }

    /** Candidato sin id, como sale de la factoria antes de guardarse. */
    public static CatalogPrice nuevoConTramo(int tierMin, Integer tierMax) {
        return CatalogPrice.create(LISTA, ARTICULO, BillingCycle.MONTHLY, tierMin, tierMax, 2,
                new BigDecimal("12000.00"), new BigDecimal("0.00"), new BigDecimal("19.00"),
                TaxTreatment.TAXED, CREADO_EL);
    }

    /** Excluido de IVA: tarifa cero, y NO es lo mismo que exento. */
    public static CatalogPrice mensualExcluido() {
        return new CatalogPrice(11L, LISTA, ARTICULO, BillingCycle.MONTHLY, 1, null, 0,
                new BigDecimal("12000.00"), new BigDecimal("0.00"), BigDecimal.ZERO,
                TaxTreatment.EXCLUDED, CREADO_EL, 0L, true);
    }
}
