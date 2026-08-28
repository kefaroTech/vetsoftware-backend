package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.subscription.domain.SubscriptionStatus;

/**
 * Telemetría de las transiciones de estado del contrato.
 *
 * <p>
 * <b>Es la métrica que más barata sale y más caro habría costado no tener.</b>
 * {@code to.status="read_only"} es un cliente al que se le acaba de cortar la
 * escritura, y su conteo absoluto —«esto debería ser cero salvo que la cobranza
 * lo haya decidido»— es exactamente el tipo de vigilancia que pide un dominio
 * de unos 500 eventos al mes: un indicador de tasa ahí no tiene muestras
 * suficientes para significar nada.
 *
 * <p>
 * <b>Una sola etiqueta, {@code to.status}, y no el par origen→destino.</b> Seis
 * estados dan 30 pares posibles, y las transiciones que importan se distinguen
 * ya por el destino: nadie pregunta «¿cuántas pasaron de ACTIVE a READ_ONLY
 * frente a las que pasaron de PAST_DUE a READ_ONLY?» mirando un panel — eso se
 * pregunta al log, donde el evento {@code subscription_status_changed} lleva
 * {@code from.status}, {@code to.status} y el actor que la provocó. Reservar la
 * alta cardinalidad para los canales que la aguantan es la regla, no una
 * excepción de este puerto.
 */
public interface SubscriptionLifecycleMetrics {

    /** Se persistió una transición de estado del contrato. */
    void statusTransitioned(SubscriptionStatus toStatus);
}
