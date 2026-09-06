package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.ChargeRequest;
import com.vetsoftware.app.paymentgateway.domain.CreatePaymentSourceRequest;
import com.vetsoftware.app.paymentgateway.domain.GatewayPaymentSource;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransaction;
import com.vetsoftware.app.paymentgateway.domain.MerchantAcceptance;
import java.time.Duration;

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
}
