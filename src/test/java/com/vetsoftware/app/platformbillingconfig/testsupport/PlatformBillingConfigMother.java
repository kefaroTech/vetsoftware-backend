package com.vetsoftware.app.platformbillingconfig.testsupport;

import com.vetsoftware.app.platformbillingconfig.domain.PlatformBillingConfig;
import com.vetsoftware.app.platformbillingconfig.domain.PriceListRef;
import java.time.LocalDateTime;

/**
 * Fixtures de la configuración de facturación de la plataforma.
 *
 * <p>
 * Los valores de aquí son de prueba y no deben leerse como los valores por
 * defecto del producto: esos viven en la fila sembrada por el changeset, que es
 * justamente el punto de la tabla.
 */
public final class PlatformBillingConfigMother {

    public static final LocalDateTime CREADA = LocalDateTime.of(2026, 1, 15, 10, 30);

    public static final PriceListRef TARIFA = new PriceListRef(7L, "LISTA-2026-01", "Tarifa 2026");

    private PlatformBillingConfigMother() {
    }

    /** La fila tal como sale de la base con una tarifa por defecto apuntada. */
    public static PlatformBillingConfig configurada() {
        return new PlatformBillingConfig(1L, TARIFA, 5, 14, 1, 5, "SIIGO", CREADA, 0L);
    }

    /**
     * La fila recién sembrada: sin tarifa por defecto y sin proveedor externo
     * decidido.
     */
    public static PlatformBillingConfig sinTarifa() {
        return new PlatformBillingConfig(1L, null, 5, 14, 1, 5, null, CREADA, 0L);
    }
}
