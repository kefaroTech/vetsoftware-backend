package com.vetsoftware.app.paymentattempt.application.dto;

import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import com.vetsoftware.app.paymentattempt.domain.PaymentAttempt;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <strong>Lleva {@code gatewayDeclineCode}, y eso es correcto aqui.</strong> El
 * DTO de aplicacion es interno: lo consumen los dos controllers, y plataforma
 * necesita el codigo crudo para revisar la traduccion. Quien decide si el dato
 * sale o no es la <em>frontera HTTP</em> — {@code PaymentAttemptResponse} no
 * declara el campo y {@code SystemPaymentAttemptResponse} si—. Poner el filtro
 * aqui obligaria a dos DTO casi iguales y dejaria a plataforma sin el dato.
 */
public record PaymentAttemptDto(Long id, Long companyId, Long billingDocumentId,
        Long paymentMethodId, int attemptNumber, String gateway, BigDecimal requestedAmount,
        String gatewayDeclineCode, DeclineKind declineKind, LocalDateTime attemptedAt,
        LocalDateTime nextAttemptAt, LocalDateTime createdDate, Long version) {

    public static PaymentAttemptDto from(PaymentAttempt attempt) {
        return new PaymentAttemptDto(attempt.getId(), attempt.getCompanyId(),
                attempt.getBillingDocumentId(), attempt.getPaymentMethodId(),
                attempt.getAttemptNumber(), attempt.getGateway(), attempt.getRequestedAmount(),
                attempt.getGatewayDeclineCode(), attempt.getDeclineKind(), attempt.getAttemptedAt(),
                attempt.getNextAttemptAt(), attempt.getCreatedDate(), attempt.getVersion());
    }
}
