package com.vetsoftware.app.subscription.application.port.out;

/**
 * Telemetría del recálculo de entitlements que dispara un cambio de contrato.
 *
 * <p>
 * <b>Por qué vive en el slice de suscripciones y no en el de entitlements.</b>
 * Lo que se mide aquí es el <em>cierre del lazo</em>: que un cambio de lo que
 * el cliente paga se convierta en un cambio de lo que el cliente puede usar. El
 * emisor es {@code EntitlementRecalculationAdapter}, que es la pieza de este
 * slice que llama al otro. Medirlo dentro de entitlements contaría también los
 * recálculos que no nacen de un cambio de contrato y mezclaría dos poblaciones.
 *
 * <p>
 * <b>Qué se pierde sin esta métrica.</b> El recálculo borra y reinserta la
 * tabla entera de permisos de la empresa dentro de la transacción del cambio de
 * contrato. Si falla, el cambio se revierte —eso está bien— pero nadie se
 * entera de que hubo un intento; y si el que falla es un lote de ellos, el
 * síntoma que llega es «varias clínicas dicen que no pueden entrar», sin
 * ninguna serie que apunte al recálculo. Es el mismo corte que ya vigila
 * {@code vetsoftware_entitlement_resolution_empty_total}, pero visto desde el
 * lado del que escribe en vez del lado del que lee.
 */
public interface SubscriptionEntitlementMetrics {

    /** El recálculo terminó bien. */
    void recalculated(Trigger trigger);

    /**
     * El recálculo lanzó. Terminal por definición: ocurre dentro de la transacción
     * del cambio de contrato, así que la excepción se lleva por delante también el
     * cambio y no hay reintento de ninguna clase.
     */
    void recalculationFailed(Trigger trigger);

    /**
     * Quién movió el contrato. Dos valores porque son dos poblaciones con dueño y
     * urgencia distintos: un pico a las tres de la mañana es el barrido nocturno
     * haciendo su trabajo; el mismo pico al mediodía son clientes esperando frente
     * a una pantalla. Con un solo valor la segunda se esconde detrás de la primera.
     */
    enum Trigger {
        /** Una persona —cliente u operador— cambió el contrato. */
        SUBSCRIPTION_CHANGED("subscription_changed"),
        /** Lo cambió un barrido programado: fin de prueba, mora, vencimiento. */
        SCHEDULED_SWEEP("scheduled_sweep");

        private final String value;

        Trigger(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
