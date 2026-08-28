package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.web.response;

import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.MandateStatus;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.PaymentMethodKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * El medio de pago tal como sale por HTTP.
 *
 * <p>
 * <strong>NO expone {@code token}, y esa ausencia es la parte importante de
 * este record.</strong> El testigo de la pasarela es la credencial con la que
 * se cobra: quien lo tiene puede mover dinero. No es un dato de presentacion y
 * no tiene ningun destinatario fuera del adaptador que habla con la pasarela.
 *
 * <p>
 * Lo que si sale es lo justo para que el cliente <em>reconozca cual es</em>
 * —{@code brand} y {@code lastFour}— y para que sepa cuando se le vence. Y por
 * supuesto tampoco existe el numero de la tarjeta: no esta en esta clase porque
 * no esta en ninguna.
 */
public record SubscriptionPaymentMethodResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PaymentMethodKind methodKind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String gateway, String brand,
        String lastFour, LocalDate expiresOn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) MandateStatus mandateStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String mandateEvidence,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime authorizedAt,
        LocalDateTime revokedAt, String revokedReason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean defaultMethod,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        Long version) {

    public static SubscriptionPaymentMethodResponse from(SubscriptionPaymentMethodDto dto) {
        return new SubscriptionPaymentMethodResponse(dto.id(), dto.companyId(), dto.methodKind(),
                dto.gateway(), dto.brand(), dto.lastFour(), dto.expiresOn(), dto.mandateStatus(),
                dto.mandateEvidence(), dto.authorizedAt(), dto.revokedAt(), dto.revokedReason(),
                dto.defaultMethod(), dto.createdDate(), dto.version());
    }
}
