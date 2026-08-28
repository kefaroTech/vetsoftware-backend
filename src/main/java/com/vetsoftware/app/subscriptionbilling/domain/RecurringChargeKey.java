package com.vetsoftware.app.subscriptionbilling.domain;

import java.time.LocalDate;

/**
 * <b>La llave que impide que un reinicio del barrido cobre dos veces.</b>
 *
 * <p>
 * El proceso mensual recorre quinientas clínicas y puede morir en la 312. Al
 * volver a arrancar vuelve a pasar por las 311 primeras, y sin una llave
 * calculada les devenga el mismo mes otra vez: no hay ninguna señal intermedia
 * que lo delate, porque el segundo cargo se ve exactamente igual de legítimo
 * que el primero. La llave convierte «ya cobré esto» en una pregunta que se le
 * puede hacer a la base antes de escribir.
 *
 * <p>
 * <b>La llave lleva la LÍNEA, no el artículo, y esto ya costó una
 * corrección.</b> Con tramos acumulativos ({@code subscription_items.tier_min}
 * / {@code tier_max}) un mismo {@code catalog_item_id} tiene <b>dos líneas
 * vivas en el mismo periodo</b> —los primeros N a una tarifa y el resto a
 * otra—, así que una llave por artículo daría las dos por duplicadas y dejaría
 * de cobrar el segundo tramo. Es la mitad del recibo, en silencio y todos los
 * meses.
 *
 * <p>
 * <b>No hay índice único que la respalde</b>: {@code subscription_charges} no
 * lleva columna de idempotencia y este proyecto no puede añadírsela desde aquí.
 * Es decir: esta regla la sostiene <em>solo el código</em>, y por eso su prueba
 * no es opcional.
 */
public record RecurringChargeKey(Long companyId, Long subscriptionId, Long subscriptionItemId,
        LocalDate periodStart, LocalDate periodEnd) {

    public RecurringChargeKey {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (subscriptionId == null)
            throw new IllegalArgumentException("subscriptionId is required");
        // La linea es obligatoria, y no por simetria: una llave sin linea agrupa los
        // dos tramos del mismo articulo y deja de cobrar el segundo.
        if (subscriptionItemId == null)
            throw new IllegalArgumentException("subscriptionItemId is required: a key without the"
                    + " contract line collapses the tiers of one catalog item into a single charge");
        if (periodStart == null || periodEnd == null)
            throw new IllegalArgumentException("service period is required");
        if (periodEnd.isBefore(periodStart))
            throw new IllegalArgumentException("periodEnd cannot be before periodStart");
    }

    /** La llave de una línea para el periodo que cierra {@code window}. */
    public static RecurringChargeKey of(Long companyId, Long subscriptionId,
            Long subscriptionItemId, ServicePeriod period) {
        if (period == null)
            throw new IllegalArgumentException("service period is required");
        return new RecurringChargeKey(companyId, subscriptionId, subscriptionItemId, period.start(),
                period.end());
    }

    /** El periodo que la llave cubre, como tipo de dominio. */
    public ServicePeriod servicePeriod() {
        return new ServicePeriod(periodStart, periodEnd);
    }

    /**
     * Forma textual estable, para el log del barrido y la bitácora.
     *
     * <p>
     * No se persiste en ninguna columna —no existe— y no debe empezar a usarse como
     * si lo fuera: es trazabilidad, no la barandilla.
     */
    public String value() {
        return "recurring:" + companyId + ":" + subscriptionId + ":" + subscriptionItemId + ":"
                + periodStart + ":" + periodEnd;
    }
}
