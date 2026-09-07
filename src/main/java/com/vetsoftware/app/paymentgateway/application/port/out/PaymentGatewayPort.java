package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.ChargeRequest;
import com.vetsoftware.app.paymentgateway.domain.CreatePaymentSourceRequest;
import com.vetsoftware.app.paymentgateway.domain.GatewayPaymentSource;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransaction;
import com.vetsoftware.app.paymentgateway.domain.MerchantAcceptance;
import java.time.Duration;
import java.util.Optional;

/**
 * La pasarela de pago. Hoy Wompi ({@code WompiGatewayClient}); un cambio de
 * proveedor es un segundo adaptador, no un cambio de caso de uso.
 *
 * <p>
 * <strong>Nunca se llama dentro de una transacción.</strong> Todas las
 * operaciones son I/O HTTP; la regla dura {@code SIN_IO_EXTERNO_EN_TRANSACCION}
 * sigue la cadena de llamadas hasta cualquier servicio que use este puerto.
 */
public interface PaymentGatewayPort {

    MerchantAcceptance fetchAcceptance();

    GatewayPaymentSource createPaymentSource(CreatePaymentSourceRequest request);

    GatewayTransaction charge(ChargeRequest request);

    GatewayTransaction findTransaction(String id);

    /**
     * Busca la transacción por la referencia propia del comercio
     * ({@code client_request_id}). La guía pública de Wompi solo documenta
     * {@link #findTransaction(String)} por el {@code id} de Wompi; esta consulta
     * usa el filtro {@code ?reference=} de {@code GET /transactions}, sin página
     * propia en esa guía. Vacío si Wompi no tiene ninguna transacción con esa
     * referencia.
     */
    Optional<GatewayTransaction> findByReference(String reference);

    /**
     * La llave pública configurada. Nunca la privada: esta sí puede llegar al
     * front.
     */
    String publicKey();

    String apiBaseUrl();

    /**
     * Cuántas veces sondear {@link #findTransaction(String)} antes de dejarlo en
     * {@code PENDING}.
     */
    int statusPollAttempts();

    /** Espera entre sondeos. */
    Duration statusPollInterval();

    /**
     * Cuanto puede seguir {@code PENDING} una transaccion antes de que la
     * conciliacion la trate como estancada.
     */
    Duration pendingTransactionMaxAge();
}
