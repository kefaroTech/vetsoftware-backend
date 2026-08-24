package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;

/**
 * <strong>El puerto por el que este slice expone que el contrato
 * cambio</strong>, para que el recalculo de permisos y contadores (R11) pueda
 * dispararse.
 *
 * <p>
 * Este slice no recalcula ni publica entitlements: eso es del slice
 * {@code entitlement}, que aporta el adaptador de este puerto. Aqui solo se
 * anuncia el hecho, y se anuncia <strong>dentro de la misma
 * transaccion</strong> que lo produjo —R11 exige que el recalculo corra en
 * ella, y no hay ningun I/O externo de por medio, asi que no aplica el diferido
 * a {@code afterCommit}—. Si algun dia el adaptador necesitara salir a la red,
 * el diferido es responsabilidad suya, no de este puerto.
 *
 * <p>
 * Se emite en: alta de contrato, alta de linea, baja de linea, cambio de
 * cantidad, cambio de estado, peticion de cancelacion y vigencias alcanzadas
 * por el reloj.
 */
public interface SubscriptionChangedPort {
    void subscriptionChanged(SubscriptionChangedEvent event);
}
