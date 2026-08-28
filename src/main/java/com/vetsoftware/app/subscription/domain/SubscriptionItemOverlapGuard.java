package com.vetsoftware.app.subscription.domain;

import java.util.List;

/**
 * La comprobacion de solape (R7), escrita una sola vez y en el dominio para que
 * los tres casos de uso que abren linea —alta, cambio de cantidad y el alta
 * inicial del contrato— no puedan implementarla cada uno a su manera.
 *
 * <p>
 * <strong>Por que hace falta codigo si el esquema ya tiene un indice
 * unico.</strong> {@code uq_subscription_items_current} sobre la columna
 * generada {@code current_item_marker} impide dos lineas <em>abiertas</em> del
 * mismo articulo en el mismo contrato, que es el caso comun y esta cerrado. Lo
 * que <strong>no</strong> impide es dos tramos con fechas de fin futuras que se
 * pisen: la linea A del 1-ene al 30-jun y la B del 1-may al 31-dic, del mismo
 * articulo, dan las dos {@code current_item_marker = NULL} y MySQL las acepta.
 * En mayo y junio ese modulo se factura dos veces. No es expresable en el motor
 * —no existen restricciones de exclusion, eso es PostgreSQL— asi que es una
 * regla que garantiza el codigo.
 *
 * <p>
 * <strong>Y por que no basta con llamar a esto.</strong> La comprobacion es un
 * <em>leer-y-luego-escribir</em>: dos transacciones concurrentes leen las dos
 * «no hay solape» y las dos insertan. Quien la llame tiene que haber tomado
 * antes el bloqueo pesimista sobre la fila de {@code subscriptions}
 * ({@code SubscriptionRepository.lockByIdAndCompanyId}), que es lo que las
 * serializa.
 */
public final class SubscriptionItemOverlapGuard {

    private SubscriptionItemOverlapGuard() {
    }

    /**
     * Rechaza el tramo candidato si se pisa con alguna de las lineas existentes. El
     * criterio de solape es el de {@link EffectivePeriod#overlaps}, intervalo
     * semiabierto, el mismo de la consulta de vigilancia.
     *
     * <p>
     * <strong>El solape se mide DENTRO del mismo tramo</strong> (D-66). Desde que
     * los tramos son acumulativos, un articulo escalonado se firma como varias
     * lineas del mismo articulo y el mismo periodo —ocho unidades a 12.000 y cinco
     * a 9.000—, que no son un cobro doble sino la particion correcta de una sola
     * cantidad. Medir el solape solo por articulo las rechazaria y dejaria la
     * aritmetica acumulativa sin poder escribirse. Es exactamente el criterio de
     * {@code uq_subscription_items_current}, que desde el 244 lleva el tramo en su
     * marcador.
     *
     * @param existing
     *            las lineas del mismo articulo, mismo contrato y misma empresa, ya
     *            excluida la que se este editando
     */
    public static void ensureNoOverlap(Long catalogItemId, int tierMin, EffectivePeriod candidate,
            List<SubscriptionItem> existing) {
        if (candidate == null)
            throw new IllegalArgumentException("candidate period is required");
        if (existing == null || existing.isEmpty())
            return;
        for (SubscriptionItem item : existing) {
            if (item.getTierMin() == tierMin && item.overlaps(candidate)) {
                throw new SubscriptionItemOverlapException(catalogItemId, candidate.from(),
                        candidate.to());
            }
        }
    }
}
