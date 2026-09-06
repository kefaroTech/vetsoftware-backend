package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.application.dto.WompiPaymentMethodDto;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Da de alta el medio de pago y lo marca como predeterminado, delegando en
 * {@code subscriptionpaymentmethod} (que es quien de verdad guarda la fila del
 * mandato).
 */
public interface PaymentMethodRegistrarPort {

    WompiPaymentMethodDto registerDefaultCard(Long companyId, String token, String brand,
            String lastFour, LocalDate expiresOn, String mandateEvidence,
            LocalDateTime authorizedAt);
}
