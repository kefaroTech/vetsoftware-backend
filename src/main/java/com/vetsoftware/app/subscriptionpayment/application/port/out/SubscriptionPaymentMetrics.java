package com.vetsoftware.app.subscriptionpayment.application.port.out;

import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;

/**
 * Telemetría del dinero que entra: pagos registrados y su imputación contra las
 * cuentas de cobro.
 *
 * <p>
 * Las dos métricas son distintas a propósito y no una con dos etiquetas. Un
 * pago recibido y una imputación no son el mismo hecho: un pago puede quedar
 * sin imputar —y esa es justamente la anomalía cara, dinero que entró y ninguna
 * factura dio por saldada— y una imputación puede no traer un peso, porque su
 * fuente sea una nota de crédito o una retención que le practicaron al cliente.
 * Con una sola métrica esa diferencia se pierde y el descuadre solo aparece en
 * la conciliación del mes siguiente.
 *
 * <p>
 * Sin {@code companyId}: ver {@code SubscriptionBillingMetrics}.
 */
public interface SubscriptionPaymentMetrics {

    /**
     * Se registró un pago. El estado de llegada es la dimensión: un pago que nace
     * {@code PENDING} todavía no es plata.
     */
    void paymentRegistered(PaymentMethod method, SubscriptionPaymentStatus status);

    /**
     * Cambió el estado de un pago ya registrado. {@code CONFIRMED → REFUNDED} es
     * plata que sale y {@code PENDING → FAILED} es plata que nunca entró: los dos
     * son eventos contables sin contador hasta ahora.
     */
    void paymentStatusChanged(PaymentMethod method, SubscriptionPaymentStatus status);

    /** Se imputó una fuente contra una cuenta de cobro. */
    void applicationRecorded(ApplicationSourceKind sourceKind);

    /**
     * Se revirtió una imputación. Va sobre el mismo contador con
     * {@code result=cancelled} por el mismo motivo que la anulación de cargos: lo
     * que se vigila es la proporción, no el conteo suelto.
     */
    void applicationReversed(ApplicationSourceKind sourceKind);
}
