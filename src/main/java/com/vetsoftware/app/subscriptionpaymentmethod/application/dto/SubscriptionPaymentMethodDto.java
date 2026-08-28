package com.vetsoftware.app.subscriptionpaymentmethod.application.dto;

import com.vetsoftware.app.subscriptionpaymentmethod.domain.MandateStatus;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.PaymentMethodKind;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethod;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Proyeccion de salida del medio de pago.
 *
 * <p>
 * <strong>Sin {@code token}.</strong> El testigo de la pasarela es la
 * credencial con la que se cobra: no es un dato de presentacion y no tiene
 * ningun destinatario fuera del adaptador que habla con la pasarela. Dejarlo
 * fuera <em>aqui</em>, y no solo en la response, es lo que impide que una
 * response futura lo recupere «porque el DTO ya lo traia».
 */
public record SubscriptionPaymentMethodDto(Long id, Long companyId, PaymentMethodKind methodKind,
        String gateway, String brand, String lastFour, LocalDate expiresOn,
        MandateStatus mandateStatus, String mandateEvidence, LocalDateTime authorizedAt,
        LocalDateTime revokedAt, String revokedReason, boolean defaultMethod,
        LocalDateTime createdDate, Long version) {

    public static SubscriptionPaymentMethodDto from(SubscriptionPaymentMethod method) {
        return new SubscriptionPaymentMethodDto(method.getId(), method.getCompanyId(),
                method.getMethodKind(), method.getGateway(), method.getBrand(),
                method.getLastFour(), method.getExpiresOn(), method.getMandateStatus(),
                method.getMandateEvidence(), method.getAuthorizedAt(), method.getRevokedAt(),
                method.getRevokedReason(), method.isDefaultMethod(), method.getCreatedDate(),
                method.getVersion());
    }
}
